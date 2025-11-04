package com.example.editphoto.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.editphoto.R
import com.example.editphoto.databinding.FragmentIconBinding
import com.example.editphoto.databinding.FragmentStickerBinding


class IconFragment : Fragment() {

    private lateinit var binding: FragmentIconBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentIconBinding.inflate(layoutInflater)
        return binding.root
    }
}