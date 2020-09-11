package ui.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whatsapp.R
import com.example.whatsapp.databinding.FragmentCameraBinding

private const val TAG = "CameraFragment"
class CameraFragment : Fragment() {
    private lateinit var fragmentCameraBinding: FragmentCameraBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG, "CAMERAAA onCreate: ")
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivity(cameraIntent)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        fragmentCameraBinding =
            DataBindingUtil.inflate(inflater , R.layout.fragment_camera , container,false)

        return fragmentCameraBinding.root
    }

    override fun onStart() {
        super.onStart()
        Log.i(TAG, "CAMERAAA onStart: ")
    }

    override fun onResume() {
        super.onResume()
        Log.i(TAG, "CAMERAAA onResume: ")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i(TAG, "CAMERAAA onViewCreated: ")

    }

}