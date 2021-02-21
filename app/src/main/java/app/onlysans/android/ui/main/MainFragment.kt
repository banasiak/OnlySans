package app.onlysans.android.ui.main

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import app.onlysans.android.R
import app.onlysans.android.data.SortOrder
import app.onlysans.android.databinding.MainFragmentBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment() {
  companion object {
    fun newInstance() = MainFragment()
  }

  private lateinit var viewModel: MainViewModel
  private lateinit var handler: Handler
  private lateinit var service: TypefaceService
  private var _binding: MainFragmentBinding? = null
  private val binding get() = _binding!!

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = MainFragmentBinding.inflate(inflater, container, false)
    return _binding!!.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    // TODO: Use the ViewModel

    val handlerThread = HandlerThread("fonts")
    handlerThread.start()
    handler = Handler(handlerThread.looper)
    service = TypefaceService(requireContext(), handler)

    viewLifecycleOwner.lifecycleScope.launch {
      val fonts = viewModel.getOnlySansFonts(SortOrder.TRENDING)
      if (fonts.isEmpty()) {
        binding.message.text = "Refractory period engaged. Try again later"
      } else {
        load(fonts.random().family)
        binding.message.setOnClickListener {
          load(fonts.random().family)
        }
      }
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  private fun load(familyName: String) {
    viewLifecycleOwner.lifecycleScope.launch {
      when (val response = service.requestTypeface(TypefaceOptions(familyName))) {
        is TypefaceResponse.Success -> {
          val firstNames = resources.getStringArray(R.array.first_names)
          binding.message.text = resources.getString(R.string.title, firstNames.random(), response.request.familyName)
          binding.message.typeface = response.typeface
          binding.preview.typeface = response.typeface
          binding.preview.isVisible = true
        }
        is TypefaceResponse.Failure -> {
          binding.message.text = "Failed, click to try again"
          binding.preview.isVisible = false
        }
      }
    }
  }
}