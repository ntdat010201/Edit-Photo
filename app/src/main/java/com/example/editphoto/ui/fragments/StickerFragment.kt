package com.example.editphoto.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.editphoto.databinding.FragmentStickerBinding

class StickerFragment : Fragment() {
    private lateinit var binding: FragmentStickerBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentStickerBinding.inflate(layoutInflater)
        return binding.root
    }

}