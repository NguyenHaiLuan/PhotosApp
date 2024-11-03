package com.example.photosapp.adapter

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView

import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.photosapp.databinding.ItemColorFilterBinding
import com.example.photosapp.utils.FilterUtils

class ColorFilterAdapter(
    private val filterList: List<String>,
    private val onFilterClick: (String) -> Unit
) : RecyclerView.Adapter<ColorFilterAdapter.ColorFilterViewHolder>() {

    private var selectedPosition = -1

    companion object {
        const val NONE = "Không"
        const val RED = "Đỏ"
        const val GREEN = "Xanh lá"
        const val BLUE = "Xanh"
        const val SEPIA = "Sepia"
        const val GRAY_SCALE = "Xám"
        const val BRIGHTENING = "Tươi sáng"
        const val CONTRAST = "Tương phản"
        const val VIBRANCY = "Ấm hơn"
    }

    inner class ColorFilterViewHolder(private val binding: ItemColorFilterBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("NotifyDataSetChanged")
        fun bind(filterName: String, isSelected: Boolean) {
            binding.filterName.text = filterName

            // Áp dụng bộ lọc xem trước cho ImageView
            when (filterName) {
                NONE -> binding.colorFiletLabel.colorFilter = FilterUtils.getNoneFilter()
                RED -> binding.colorFiletLabel.colorFilter = FilterUtils.getRedFilter()
                GREEN -> binding.colorFiletLabel.colorFilter = FilterUtils.getGreenFilter()
                BLUE -> binding.colorFiletLabel.colorFilter = FilterUtils.getBlueFilter()
                SEPIA -> binding.colorFiletLabel.colorFilter = FilterUtils.getSepiaFilter()
                GRAY_SCALE -> binding.colorFiletLabel.colorFilter = FilterUtils.getGrayScaleFilter()
                BRIGHTENING -> binding.colorFiletLabel.colorFilter = FilterUtils.getBrighteningFilter(20f)
                CONTRAST -> binding.colorFiletLabel.colorFilter = FilterUtils.getContrastFilter(5f)
                VIBRANCY -> binding.colorFiletLabel.colorFilter = FilterUtils.getVibranceFilter(2f)
                else -> binding.colorFiletLabel.clearColorFilter()
            }

            // Đổi trạng thái chọn và cập nhật UI
            binding.root.isSelected = isSelected
            binding.root.setOnClickListener {
                onFilterClick(filterName)
                selectedPosition = adapterPosition
                notifyDataSetChanged()
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorFilterViewHolder {
        val binding = ItemColorFilterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ColorFilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ColorFilterViewHolder, position: Int) {
        holder.bind(filterList[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = filterList.size
}