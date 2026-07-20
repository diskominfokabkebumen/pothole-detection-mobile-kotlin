package com.example.hallohalloapp.ui

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.hallohalloapp.R
import com.example.hallohalloapp.detection.storage.PotholeStorage
import com.example.hallohalloapp.detection.storage.UserProfileStorage
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.btnEditProfile)?.setOnClickListener {
            showEditProfileDialog(view)
        }

        updateProfileUI(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { updateProfileUI(it) }
    }

    private fun updateProfileUI(rootView: View) {
        rootView.findViewById<TextView>(R.id.tvProfileName)?.text =
            UserProfileStorage.getName(requireContext())
        rootView.findViewById<TextView>(R.id.tvProfileEmail)?.text =
            UserProfileStorage.getEmail(requireContext())

        val records = PotholeStorage.loadAll(requireContext())

        rootView.findViewById<TextView>(R.id.tvTotalLaporanProfile)?.text =
            records.size.toString()

        val avgConfidence = if (records.isNotEmpty()) {
            (records.map { it.confidence }.average() * 100).toInt()
        } else {
            0
        }
        rootView.findViewById<TextView>(R.id.tvAvgConfidenceProfile)?.text = "$avgConfidence%"
    }

    private fun showEditProfileDialog(rootView: View) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_profile, null)

        val etName = dialogView.findViewById<TextInputEditText>(R.id.etEditName)
        val etEmail = dialogView.findViewById<TextInputEditText>(R.id.etEditEmail)

        etName.setText(UserProfileStorage.getName(requireContext()))
        etEmail.setText(UserProfileStorage.getEmail(requireContext()))

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnCancelEditProfile).setOnClickListener {
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnSaveEditProfile).setOnClickListener {
            val newName = etName.text?.toString()?.trim().orEmpty()
            val newEmail = etEmail.text?.toString()?.trim().orEmpty()

            if (newName.isEmpty()) {
                etName.error = "Nama tidak boleh kosong"
                return@setOnClickListener
            }

            UserProfileStorage.saveProfile(requireContext(), newName, newEmail)
            updateProfileUI(rootView)
            Toast.makeText(requireContext(), "Profil berhasil diperbarui", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        dialog.show()
    }
}