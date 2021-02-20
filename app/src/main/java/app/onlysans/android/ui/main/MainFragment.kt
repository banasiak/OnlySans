package app.onlysans.android.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import app.onlysans.android.R

class MainFragment : Fragment() {
  companion object {
    fun newInstance() = MainFragment()
  }

  private lateinit var viewModel: MainViewModel
  private lateinit var handler: Handler

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {
    return inflater.inflate(R.layout.main_fragment, container, false)
  }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
    // TODO: Use the ViewModel

    val handlerThread = HandlerThread("fonts")
    handlerThread.start()
    handler = Handler(handlerThread.looper)

    view!!.findViewById<TextView>(R.id.message)?.applyFont(
      handler = handler,
      fontOptions = FontOptions(familyName = "Cherry Cream Soda"),
      success = {
        it.text = "Abella Ipsum"
      }
    )
  }

}