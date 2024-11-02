package com.example.photosapp.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.photosapp.R
import com.example.photosapp.databinding.DialogRenameMediaBinding
import io.github.muddz.styleabletoast.StyleableToast

class RenameMediaDialog : DialogFragment() {

    private var _binding: DialogRenameMediaBinding? = null
    private val binding get() = _binding!!

    interface RenameMediaListener {
        fun onRenameConfirmed(newName: String)
    }

    private var renameMediaListener: RenameMediaListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is RenameMediaListener) {
            renameMediaListener = context
        } else {
            throw RuntimeException("$context must implement RenameMediaListener")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRenameMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnConfirm.setOnClickListener {
            val newName = binding.editTextNewName.text.toString().trim()
            if (newName.isNotEmpty()) {
                renameMediaListener?.onRenameConfirmed(newName)
                dismiss()
            } else {
                StyleableToast.makeText(requireContext(), "Tên không được để trống", R.style.warning_toast).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
