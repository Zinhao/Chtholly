package com.zinhao.chtholly.view

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.zinhao.chtholly.BotApp
import com.zinhao.chtholly.databinding.FragmentSetupFeishuBinding
import com.zinhao.chtholly.viewmodel.SetupViewModel

class SetupFeiShuFragment : Fragment() {
    private lateinit var binding: FragmentSetupFeishuBinding
    private lateinit var viewModel: SetupViewModel
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSetupFeishuBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SetupViewModel::class.java]
        binding.etAppId.setText(BotApp.getInstance().feishuAppId)
        binding.etSecret.setText(BotApp.getInstance().feishuAppSecret)
        binding.etAppId.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateFeishuAppId(s.toString().trim())
            }
        })
        binding.etSecret.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                viewModel.updateFeishuAppSecret(s.toString().trim())
            }
        })
    }
}