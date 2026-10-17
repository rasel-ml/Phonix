package app.mrm.phonix.lib.ui.list

import androidx.core.view.isVisible
import app.mrm.phonix.R
import app.mrm.phonix.lib.adapter.ListAdapter
import app.mrm.phonix.lib.ui.widgets.FastScrollerTouchGate
import app.mrm.phonix.databinding.ItemsBinding
import app.mrm.phonix.lib.ui.permissioned.PermissionedFragment

abstract class ListFragment<ItemType, VS : ListViewState<ItemType>> : PermissionedFragment<VS>() {
    override val mainContentView by lazy { binding.root }

    protected val binding by lazy { ItemsBinding.inflate(layoutInflater) }


    override fun onSetup() {
        viewState.apply {
            isEmpty.observe(this@ListFragment, this@ListFragment::showEmpty)
            emptyMessage.observe(this@ListFragment, binding.empty.emptyText::setText)
            emptyIcon.observe(this@ListFragment, binding.empty.emptyIcon::setImageResource)

            items.observe(this@ListFragment) {
                adapter.items = it
            }

            filter.observe(this@ListFragment) {
                adapter.titleFilter = it
            }

            isLoading.observe(this@ListFragment) {
                if (it) showEmpty(false)
            }

            isScrollerVisible.observe(this@ListFragment) {
                binding.itemsScrollView.fastScroller.isVisible = it
            }

            onIsObserve(args.getBoolean(ARG_OBSERVE, true))
        }

        adapter.apply {
            setOnItemClickListener(viewState::onItemClick)
            setOnItemLongClickListener(viewState::onItemLongClick)
            setOnItemLeftButtonClickListener(viewState::onItemLeftClick)
            setOnItemRightButtonClickListener(viewState::onItemRightClick)
            binding.itemsScrollView.setAdapter(this)
        }

        binding.itemsScrollView.fastScroller.apply {
            // hugging the screen edge, clear of the rows' own buttons
            setPadding(0, 0, resources.getDimensionPixelSize(R.dimen.fast_scroller_padding_end), 0)
            // only the thumb starts a scroll drag, the rest of the edge belongs to the list
            FastScrollerTouchGate(this, binding.itemsScrollView).attach()
        }
        args.getString(ARG_FILTER)?.let(viewState::onFilterChanged)
    }

    protected open fun showEmpty(isShow: Boolean) {
        binding.apply {
            empty.emptyIcon.isVisible = isShow
            empty.emptyText.isVisible = isShow
            itemsScrollView.isVisible = !isShow
        }
    }


    abstract val adapter: ListAdapter<ItemType>

    companion object {
        const val ARG_FILTER = "filter"
        const val ARG_OBSERVE = "is_observe"
    }
}