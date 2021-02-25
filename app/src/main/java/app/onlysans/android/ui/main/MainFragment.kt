package app.onlysans.android.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import app.onlysans.android.R
import app.onlysans.android.data.Font
import app.onlysans.android.data.SortOrder
import app.onlysans.android.databinding.MainFragmentBinding
import app.onlysans.android.typeface.TypefaceOptions
import app.onlysans.android.typeface.TypefaceResponse
import app.onlysans.android.typeface.TypefaceService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainFragment : Fragment(), AdapterView.OnItemSelectedListener {

  companion object {
    fun newInstance() = MainFragment()
  }

  @Inject lateinit var typefaceService: TypefaceService

  private lateinit var viewModel: MainViewModel

  private var _binding: MainFragmentBinding? = null
  private val binding get() = requireNotNull(_binding)
  private var fonts = listOf<Font>()

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    _binding = MainFragmentBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    // TODO: Use the ViewModel

    viewLifecycleOwner.lifecycleScope.launch {
      fonts = viewModel.getOnlySansFonts(SortOrder.ALPHA)
      val adapter = ArrayAdapter<Font>(
        this@MainFragment.requireContext(),
        R.layout.support_simple_spinner_dropdown_item
      ).apply { addAll(fonts) }
      binding.fonts.adapter = adapter
      binding.fonts.isVisible = true
      binding.fonts.onItemSelectedListener = this@MainFragment
      binding.loading.isVisible = false
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  private fun load(familyName: String) {
    viewLifecycleOwner.lifecycleScope.launch {
      when (val response = typefaceService.requestTypeface(TypefaceOptions(familyName))) {
        is TypefaceResponse.Success -> {
          val firstNames = resources.getStringArray(R.array.first_names)
          binding.title.text = resources.getString(R.string.title, firstNames.random(), response.request.familyName)
          binding.title.typeface = response.typeface
          binding.title.isVisible = true
          binding.preview.typeface = response.typeface
          binding.preview.isVisible = true
        }
        is TypefaceResponse.Failure -> {
          Toast.makeText(this@MainFragment.requireContext(), "Failed to load font. Choose again.", Toast.LENGTH_LONG)
            .show()
          binding.title.isVisible = false
          binding.preview.isVisible = false
        }
      }
    }
  }

  override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
    val selectedFont = fonts[position]
    load(selectedFont.family)
  }

  override fun onNothingSelected(parent: AdapterView<*>?) {
    // no-op
  }
}