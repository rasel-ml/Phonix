package app.mrm.phonix.lib.ui.contacteditor

import android.Manifest.permission.READ_CONTACTS
import android.Manifest.permission.WRITE_CONTACTS
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.ColorStateList
import android.net.Uri
import android.text.InputType
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import app.mrm.phonix.R
import app.mrm.phonix.databinding.ContactEditorBinding
import app.mrm.phonix.databinding.EditorChoiceItemBinding
import app.mrm.phonix.databinding.EditorChoiceSheetBinding
import app.mrm.phonix.databinding.EditorInputBinding
import app.mrm.phonix.databinding.EditorRowBinding
import app.mrm.phonix.databinding.EditorSectionBinding
import app.mrm.phonix.lib.ui.base.BaseActivity
import app.mrm.phonix.lib.util.getAttrColor
import app.mrm.phonix.lib.util.getSizeInDp
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import java.text.DateFormat
import java.util.Calendar
import java.util.EnumMap
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

/**
 * Full-screen editor used to create a new contact or to edit an existing one.
 *
 * Every kind of field the contacts provider can store (see [FieldKind]) can be added, typed
 * (mobile / home / work / custom label…) and removed here.
 */
@AndroidEntryPoint
class ContactEditorActivity : BaseActivity<ContactEditorViewState>() {
    override val contentView by lazy { binding.root }
    override val viewState: ContactEditorViewState by viewModels()

    private val binding by lazy { ContactEditorBinding.inflate(layoutInflater) }

    private val sections = EnumMap<FieldKind, Section>(FieldKind::class.java)
    private val chips = EnumMap<FieldKind, Chip>(FieldKind::class.java)

    private lateinit var prefixEdit: TextInputEditText
    private lateinit var firstNameEdit: TextInputEditText
    private lateinit var middleNameEdit: TextInputEditText
    private lateinit var lastNameEdit: TextInputEditText
    private lateinit var suffixEdit: TextInputEditText
    private lateinit var nicknameEdit: TextInputEditText
    private lateinit var phoneticFirstEdit: TextInputEditText
    private lateinit var phoneticMiddleEdit: TextInputEditText
    private lateinit var phoneticLastEdit: TextInputEditText

    private val cropPhoto = registerForActivityResult(CropPhotoActivity.Contract()) { uri ->
        uri?.let(viewState::onPhotoPicked)
    }

    private val pickPhoto = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { cropPhoto.launch(it) }
    }

    private val requestPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) {
                startEditor()
            } else {
                showError(R.string.editor_error_permission)
                finish()
            }
        }

    private class RowHolder(
        val kind: FieldKind,
        val binding: EditorRowBinding,
        val inputs: List<TextInputEditText>,
        var type: Int,
        var label: String?,
        var extras: Map<String, String?>,
        var date: String? = null
    )

    private class Section(val kind: FieldKind, val binding: EditorSectionBinding) {
        val rows = mutableListOf<RowHolder>()
    }

    private class DateInfo(val year: Int?, val month: Int, val day: Int)


    override fun onSetup() {
        binding.apply {
            editorClose.setOnClickListener { onCloseRequested() }
            editorSave.setOnClickListener { onSaveClick() }
            editorDelete.setOnClickListener { onDeleteClick() }
            editorStar.setOnClickListener { editorStar.isActivated = !editorStar.isActivated }
            editorPhoto.setOnClickListener { onPhotoClick() }
            editorPhotoEdit.setOnClickListener { onPhotoClick() }
            editorNameMoreToggle.setOnClickListener {
                setMoreNameFields(!editorNameMoreFields.isVisible)
            }
        }

        buildNameFields()
        buildSections()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onCloseRequested()
            }
        })

        viewState.apply {
            draft.observe(this@ContactEditorActivity) { populate(it) }
            photo.observe(this@ContactEditorActivity) { refreshPhoto() }
            accounts.observe(this@ContactEditorActivity) { updateAccountButton() }
            isSaving.observe(this@ContactEditorActivity) { binding.editorSave.isEnabled = !it }
            savedEvent.observe(this@ContactEditorActivity) { ev ->
                ev.ifNew?.let { finish() }
            }
        }

        val permissions = arrayOf(READ_CONTACTS, WRITE_CONTACTS)
        if (permissions.all { ContextCompat.checkSelfPermission(this, it) == PERMISSION_GRANTED }) {
            startEditor()
        } else {
            requestPermissions.launch(permissions)
        }
    }

    override fun onSaveInstanceState(outState: android.os.Bundle) {
        // keep what has been typed if the activity is recreated (the view model outlives it)
        viewState.currentDraft?.let(::collectDraft)
        super.onSaveInstanceState(outState)
    }

    private fun startEditor() {
        val contactId = intent.getLongExtra(EXTRA_CONTACT_ID, -1L).takeIf { it != -1L }
        binding.editorTitle.setText(
            if (contactId == null) R.string.editor_title_new else R.string.editor_title_edit
        )
        viewState.onStart(contactId, intent.getStringExtra(EXTRA_NUMBER))
    }

    // region building the form

    private fun buildNameFields() {
        firstNameEdit = addNameInput(binding.editorNameFields, R.string.field_first_name)
        lastNameEdit = addNameInput(binding.editorNameFields, R.string.field_last_name)
        prefixEdit = addNameInput(binding.editorNameMoreFields, R.string.field_prefix)
        middleNameEdit = addNameInput(binding.editorNameMoreFields, R.string.field_middle_name)
        suffixEdit = addNameInput(binding.editorNameMoreFields, R.string.field_suffix)
        nicknameEdit = addNameInput(binding.editorNameMoreFields, R.string.hint_nickname)

        val caption = TextView(this).apply {
            setText(R.string.field_phonetic_title)
            setTextAppearance(R.style.Chooloo_Text_Title_Small)
            setTextColor(getAttrColor(R.attr.colorOnSurfaceVariant))
        }
        binding.editorNameMoreFields.addView(
            caption,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = getSizeInDp(8)
                bottomMargin = getSizeInDp(8)
            }
        )

        phoneticFirstEdit = addNameInput(binding.editorNameMoreFields, R.string.field_phonetic_first)
        phoneticMiddleEdit = addNameInput(binding.editorNameMoreFields, R.string.field_phonetic_middle)
        phoneticLastEdit = addNameInput(binding.editorNameMoreFields, R.string.field_phonetic_last)
    }

    private fun addNameInput(parent: LinearLayout, @StringRes hintRes: Int): TextInputEditText {
        val input = EditorInputBinding.inflate(layoutInflater, parent, false)
        input.root.hint = getString(hintRes)
        input.editorInputEdit.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS or
                    InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        parent.addView(input.root)
        return input.editorInputEdit
    }

    private fun setMoreNameFields(show: Boolean) {
        binding.editorNameMoreFields.isVisible = show
        binding.editorNameMoreToggle.setText(
            if (show) R.string.editor_less_name_fields else R.string.editor_more_name_fields
        )
    }

    private fun buildSections() {
        FieldKind.values().forEach { kind ->
            val sectionBinding =
                EditorSectionBinding.inflate(layoutInflater, binding.editorSections, false)
            sectionBinding.apply {
                editorSectionIcon.setImageResource(kind.iconRes)
                editorSectionTitle.setText(kind.titleRes)
                editorSectionAdd.setText(if (kind.alwaysShown) kind.addRes else R.string.add_another)
                editorSectionAdd.isVisible = kind.multiple
                editorSectionAdd.setOnClickListener { addRow(kind, focus = true) }
                root.isVisible = false
            }
            binding.editorSections.addView(sectionBinding.root)
            sections[kind] = Section(kind, sectionBinding)

            if (!kind.alwaysShown) {
                val chip = Chip(this).apply {
                    setText(kind.addRes)
                    chipIcon = ContextCompat.getDrawable(this@ContactEditorActivity, kind.iconRes)
                    isChipIconVisible = true
                    isCheckable = false
                    chipBackgroundColor =
                        ColorStateList.valueOf(getAttrColor(R.attr.colorSecondaryContainer))
                    chipIconTint = ColorStateList.valueOf(getAttrColor(R.attr.colorOnSecondaryContainer))
                    setTextColor(getAttrColor(R.attr.colorOnSecondaryContainer))
                    chipStrokeWidth = 0f
                    setOnClickListener { addRow(kind, focus = true) }
                }
                chips[kind] = chip
                binding.editorMoreChips.addView(chip)
            }
        }
    }

    // endregion

    // region draft <-> form

    private fun populate(draft: ContactDraft) {
        firstNameEdit.setText(draft.firstName)
        lastNameEdit.setText(draft.lastName)
        prefixEdit.setText(draft.prefix)
        middleNameEdit.setText(draft.middleName)
        suffixEdit.setText(draft.suffix)
        nicknameEdit.setText(draft.nickname)
        phoneticFirstEdit.setText(draft.phoneticFirst)
        phoneticMiddleEdit.setText(draft.phoneticMiddle)
        phoneticLastEdit.setText(draft.phoneticLast)
        setMoreNameFields(
            listOf(
                draft.prefix, draft.middleName, draft.suffix, draft.nickname,
                draft.phoneticFirst, draft.phoneticMiddle, draft.phoneticLast
            ).any { it.isNotBlank() }
        )
        binding.editorStar.isActivated = draft.starred
        binding.editorDelete.isVisible = !draft.isNew

        sections.values.forEach {
            it.rows.clear()
            it.binding.editorSectionRows.removeAllViews()
        }

        FieldKind.values().forEach { kind ->
            draft.fields[kind].orEmpty().forEach { addRow(kind, it) }
            if (kind.alwaysShown && sections.getValue(kind).rows.isEmpty()) addRow(kind)
            updateSectionVisibility(kind)
        }

        updateAccountButton()
        refreshPhoto()

        if (viewState.initialSignature == null) {
            collectDraft(draft)
            viewState.initialSignature = draft.signature()
        }
    }

    private fun collectDraft(draft: ContactDraft) {
        draft.firstName = firstNameEdit.text?.toString().orEmpty()
        draft.lastName = lastNameEdit.text?.toString().orEmpty()
        draft.prefix = prefixEdit.text?.toString().orEmpty()
        draft.middleName = middleNameEdit.text?.toString().orEmpty()
        draft.suffix = suffixEdit.text?.toString().orEmpty()
        draft.nickname = nicknameEdit.text?.toString().orEmpty()
        draft.phoneticFirst = phoneticFirstEdit.text?.toString().orEmpty()
        draft.phoneticMiddle = phoneticMiddleEdit.text?.toString().orEmpty()
        draft.phoneticLast = phoneticLastEdit.text?.toString().orEmpty()
        draft.starred = binding.editorStar.isActivated

        draft.fields.clear()
        sections.values.forEach { section ->
            section.rows.forEach { row ->
                val values = row.inputs.map { it.text?.toString().orEmpty().trim() }.toMutableList()
                if (row.kind.isDate) values[0] = row.date.orEmpty()
                if (values.all { it.isEmpty() }) return@forEach
                draft.entries(row.kind).add(FieldEntry(values, row.type, row.label, row.extras))
            }
        }
    }

    // endregion

    // region rows

    private fun addRow(kind: FieldKind, entry: FieldEntry? = null, focus: Boolean = false) {
        val section = sections.getValue(kind)
        val row = EditorRowBinding.inflate(layoutInflater, section.binding.editorSectionRows, false)

        val inputs = kind.hintRes.indices.map { i ->
            val input = EditorInputBinding.inflate(layoutInflater, row.editorRowFields, false)
            input.root.hint = getString(kind.hintRes[i])
            input.editorInputEdit.apply {
                inputType = kind.inputTypes[i]
                if (kind.isMultiline) {
                    minLines = 3
                    gravity = Gravity.TOP or Gravity.START
                }
                setText(entry?.values?.getOrNull(i).orEmpty())
            }
            row.editorRowFields.addView(input.root)
            input.editorInputEdit
        }

        val holder = RowHolder(
            kind = kind,
            binding = row,
            inputs = inputs,
            type = entry?.type ?: kind.defaultType,
            label = entry?.label,
            extras = entry?.extras ?: emptyMap(),
            date = if (kind.isDate) entry?.values?.getOrNull(0)?.takeIf { it.isNotBlank() } else null
        )

        if (kind.hasType) {
            row.editorRowType.setOnClickListener { chooseType(holder) }
            updateTypeText(holder)
        } else {
            row.editorRowType.isVisible = false
        }

        if (kind.isDate) {
            inputs[0].apply {
                inputType = InputType.TYPE_NULL
                isFocusable = false
                isCursorVisible = false
                setOnClickListener { pickDate(holder) }
            }
            row.editorRowExtra.isVisible = true
            row.editorRowExtra.setOnClickListener { toggleYear(holder, row.editorRowExtra.isChecked) }
            showDate(holder)
        }

        row.editorRowRemove.setOnClickListener { removeRow(holder) }

        section.rows.add(holder)
        section.binding.editorSectionRows.addView(row.root)
        updateSectionVisibility(kind)

        if (focus) {
            if (kind.isDate && holder.date == null) {
                pickDate(holder)
            } else {
                inputs.first().apply {
                    requestFocus()
                    post {
                        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                            .showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                    }
                }
            }
        }
    }

    private fun removeRow(row: RowHolder) {
        val section = sections.getValue(row.kind)
        section.rows.remove(row)
        section.binding.editorSectionRows.removeView(row.binding.root)
        if (section.rows.isEmpty() && row.kind.alwaysShown) addRow(row.kind)
        updateSectionVisibility(row.kind)
    }

    private fun updateSectionVisibility(kind: FieldKind) {
        val section = sections.getValue(kind)
        val hasRows = section.rows.isNotEmpty()
        section.binding.root.isVisible = hasRows
        chips[kind]?.isVisible = !hasRows
        binding.editorMoreTitle.isVisible = chips.values.any { it.isVisible }
        binding.editorMoreChips.isVisible = binding.editorMoreTitle.isVisible
    }

    // endregion

    // region type & custom label

    private fun updateTypeText(row: RowHolder) {
        val kind = row.kind
        row.binding.editorRowType.text = when {
            row.type == kind.customType ->
                row.label?.takeIf { it.isNotBlank() }
                    ?: getString(R.string.editor_custom).removeSuffix("…")
            else -> (kind.types + kind.legacyTypes).firstOrNull { it.value == row.type }
                ?.let { getString(it.labelRes) }
                ?: getString(R.string.type_other)
        }
    }

    private fun chooseType(row: RowHolder) {
        val kind = row.kind
        val choices = kind.types.map { getString(it.labelRes) } + getString(R.string.editor_custom)
        val selected = when {
            row.type == kind.customType && !row.label.isNullOrBlank() -> choices.lastIndex
            else -> kind.types.indexOfFirst { it.value == row.type }
        }

        showChoiceSheet(kind.titleRes, choices, selected) { index ->
            if (index < kind.types.size) {
                row.type = kind.types[index].value
                row.label = null
                updateTypeText(row)
            } else {
                askCustomLabel(row.label) { label ->
                    row.type = kind.customType
                    row.label = label
                    updateTypeText(row)
                }
            }
        }
    }

    private fun askCustomLabel(current: String?, onLabel: (String) -> Unit) {
        val input = EditorInputBinding.inflate(layoutInflater)
        input.root.hint = getString(R.string.editor_custom_label_hint)
        input.editorInputEdit.inputType =
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        input.editorInputEdit.setText(current.orEmpty())

        val container = FrameLayout(this).apply {
            val side = getSizeInDp(24)
            setPadding(side, getSizeInDp(8), side, 0)
            addView(input.root)
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.editor_custom_label_title)
            .setView(container)
            .setPositiveButton(R.string.editor_ok) { _, _ ->
                input.editorInputEdit.text?.toString()?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?.let(onLabel)
            }
            .setNegativeButton(R.string.editor_cancel, null)
            .show()
    }

    private fun showChoiceSheet(
        @StringRes titleRes: Int,
        choices: List<String>,
        selectedIndex: Int,
        onChoice: (Int) -> Unit
    ) {
        val dialog = BottomSheetDialog(this)
        val sheet = EditorChoiceSheetBinding.inflate(layoutInflater)
        sheet.editorChoiceTitle.setText(titleRes)

        choices.forEachIndexed { index, text ->
            val item = EditorChoiceItemBinding.inflate(layoutInflater, sheet.editorChoiceItems, false)
            item.root.text = text
            if (index == selectedIndex) {
                item.root.backgroundTintList =
                    ColorStateList.valueOf(getAttrColor(R.attr.colorSecondaryContainer))
                item.root.setTextColor(getAttrColor(R.attr.colorOnSecondaryContainer))
            }
            item.root.setOnClickListener {
                dialog.dismiss()
                onChoice(index)
            }
            sheet.editorChoiceItems.addView(item.root)
        }

        dialog.setContentView(sheet.root)
        dialog.behavior.skipCollapsed = true
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }

    // endregion

    // region dates

    private fun parseDate(raw: String?): DateInfo? {
        if (raw.isNullOrBlank()) return null
        REGEX_FULL_DATE.find(raw)?.let {
            val (y, m, d) = it.destructured
            return DateInfo(y.toInt(), m.toInt(), d.toInt())
        }
        REGEX_NO_YEAR_DATE.find(raw)?.let {
            val (m, d) = it.destructured
            return DateInfo(null, m.toInt(), d.toInt())
        }
        REGEX_COMPACT_DATE.find(raw)?.let {
            val (y, m, d) = it.destructured
            return DateInfo(y.toInt(), m.toInt(), d.toInt())
        }
        return null
    }

    private fun formatDate(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val info = parseDate(raw) ?: return raw
        return try {
            if (info.year != null) {
                DateFormat.getDateInstance(DateFormat.LONG)
                    .format(GregorianCalendar(info.year, info.month - 1, info.day).time)
            } else {
                val pattern = android.text.format.DateFormat
                    .getBestDateTimePattern(Locale.getDefault(), "MMMMd")
                java.text.SimpleDateFormat(pattern, Locale.getDefault())
                    .format(GregorianCalendar(2000, info.month - 1, info.day).time)
            }
        } catch (e: Exception) {
            raw
        }
    }

    private fun showDate(row: RowHolder) {
        row.inputs[0].setText(formatDate(row.date))
        row.binding.editorRowExtra.isChecked = row.date?.startsWith("--") == true
    }

    private fun toggleYear(row: RowHolder, withoutYear: Boolean) {
        val info = parseDate(row.date) ?: return
        row.date = if (withoutYear) {
            String.format(Locale.US, "--%02d-%02d", info.month, info.day)
        } else {
            val year = info.year ?: Calendar.getInstance().get(Calendar.YEAR)
            String.format(Locale.US, "%04d-%02d-%02d", year, info.month, info.day)
        }
        showDate(row)
    }

    private fun pickDate(row: RowHolder) {
        val utc = TimeZone.getTimeZone("UTC")
        val selection = Calendar.getInstance(utc).apply {
            parseDate(row.date)?.let {
                set(it.year ?: get(Calendar.YEAR), it.month - 1, it.day, 12, 0, 0)
            }
        }.timeInMillis

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.editor_pick_date)
            .setSelection(selection)
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            val picked = Calendar.getInstance(utc).apply { timeInMillis = millis }
            val month = picked.get(Calendar.MONTH) + 1
            val day = picked.get(Calendar.DAY_OF_MONTH)
            row.date = if (row.binding.editorRowExtra.isChecked) {
                String.format(Locale.US, "--%02d-%02d", month, day)
            } else {
                String.format(Locale.US, "%04d-%02d-%02d", picked.get(Calendar.YEAR), month, day)
            }
            showDate(row)
        }
        picker.show(supportFragmentManager, null)
    }

    // endregion

    // region photo & account

    private fun onPhotoClick() {
        val draft = viewState.currentDraft ?: return
        val hasPhoto = draft.newPhoto != null || (!draft.photoRemoved && draft.existingPhotoUri != null)
        val choices = mutableListOf(getString(R.string.editor_choose_photo))
        if (hasPhoto) choices += getString(R.string.editor_remove_photo)

        showChoiceSheet(R.string.editor_photo, choices, -1) {
            if (it == 0) pickPhoto.launch("image/*") else viewState.onPhotoRemoved()
        }
    }

    private fun refreshPhoto() {
        val draft = viewState.currentDraft ?: return
        val image = binding.editorPhoto
        val padding = getSizeInDp(28)

        fun showPlaceholder() {
            image.setImageResource(R.drawable.person)
            image.imageTintList = ColorStateList.valueOf(getAttrColor(R.attr.colorOnSurfaceVariant))
            image.setPadding(padding, padding, padding, padding)
        }

        fun showPhotoMode() {
            image.imageTintList = null
            image.setPadding(0, 0, 0, 0)
        }

        val newPhoto = draft.newPhoto
        val existing = draft.existingPhotoUri
        when {
            newPhoto != null -> {
                showPhotoMode()
                image.setImageBitmap(newPhoto)
            }
            !draft.photoRemoved && existing != null -> {
                showPhotoMode()
                Picasso.with(this).load(Uri.parse(existing)).into(image, object : Callback {
                    override fun onSuccess() {}
                    override fun onError() = showPlaceholder()
                })
            }
            else -> showPlaceholder()
        }
    }

    private fun accountLabel(account: AccountOption) =
        if (account.isDevice) getString(R.string.editor_account_device) else account.name.orEmpty()

    private fun updateAccountButton() {
        val draft = viewState.currentDraft
        val accounts = viewState.accounts.value.orEmpty()
        val button = binding.editorAccount

        button.isVisible = draft != null && draft.isNew && accounts.size > 1
        if (draft == null) return

        button.text = getString(R.string.editor_save_to, accountLabel(draft.account))
        button.setOnClickListener {
            showChoiceSheet(
                R.string.editor_account_title,
                accounts.map(::accountLabel),
                accounts.indexOf(draft.account)
            ) {
                viewState.onAccountSelected(accounts[it])
                updateAccountButton()
            }
        }
    }

    // endregion

    // region actions

    private fun onSaveClick() {
        val draft = viewState.currentDraft ?: return
        collectDraft(draft)
        viewState.onSave()
    }

    private fun onDeleteClick() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.action_delete_contact)
            .setMessage(R.string.explain_delete_contact)
            .setPositiveButton(R.string.editor_delete_confirm) { _, _ -> viewState.onDelete() }
            .setNegativeButton(R.string.editor_cancel, null)
            .show()
    }

    private fun onCloseRequested() {
        val draft = viewState.currentDraft
        if (draft == null) {
            finish()
            return
        }

        collectDraft(draft)
        if (draft.signature() == viewState.initialSignature) {
            finish()
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.editor_discard_title)
                .setMessage(R.string.editor_discard_message)
                .setPositiveButton(R.string.editor_discard) { _, _ -> finish() }
                .setNegativeButton(R.string.editor_keep_editing, null)
                .show()
        }
    }

    // endregion

    companion object {
        private const val EXTRA_CONTACT_ID = "contact_id"
        private const val EXTRA_NUMBER = "number"

        private val REGEX_FULL_DATE = Regex("^(\\d{4})-(\\d{1,2})-(\\d{1,2})")
        private val REGEX_NO_YEAR_DATE = Regex("^--(\\d{1,2})-(\\d{1,2})")
        private val REGEX_COMPACT_DATE = Regex("^(\\d{4})(\\d{2})(\\d{2})$")

        /** @param contactId the contact to edit, or null to create a new one */
        fun newIntent(context: Context, contactId: Long? = null, number: String? = null) =
            Intent(context, ContactEditorActivity::class.java).apply {
                contactId?.let { putExtra(EXTRA_CONTACT_ID, it) }
                number?.let { putExtra(EXTRA_NUMBER, it) }
            }
    }
}
