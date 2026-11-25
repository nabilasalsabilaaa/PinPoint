package com.example.pinpoint.user

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.pinpoint.Login
import com.example.pinpoint.R
import com.example.pinpoint.admin.BantuanActivity
import com.example.pinpoint.admin.BiodataActivity
import com.example.pinpoint.admin.CropRotateImageView
import com.example.pinpoint.admin.KeamananAkunActivity
import com.example.pinpoint.admin.TentangActivity
import com.example.pinpoint.network.ApiClient
import com.example.pinpoint.network.UserAccountDto
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

class ProfilActivity : AppCompatActivity() {

    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var ivProfileAvatar: ImageView
    private lateinit var btnEditPhoto: ImageView
    private lateinit var menuBiodata: RelativeLayout
    private lateinit var menuKeamanan: RelativeLayout
    private lateinit var menuBantuan: RelativeLayout
    private lateinit var menuTentang: RelativeLayout
    private lateinit var btnLogoutUser: Button

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val imageUri: Uri? = result.data?.data
            if (imageUri != null) {
                showCropRotateDialog(imageUri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profil)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainProfil)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        initViews()
        setupMenuClickListeners()
        setupBottomNavigation()
        btnLogoutUser.setOnClickListener { performLogout() }
    }

    override fun onResume() {
        super.onResume()
        displayUserInfo()
    }

    private fun initViews() {
        tvProfileName = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar)
        btnEditPhoto = findViewById(R.id.btnEditPhoto)
        menuBiodata = findViewById(R.id.menuBiodata)
        menuKeamanan = findViewById(R.id.menuKeamanan)
        menuBantuan = findViewById(R.id.menuBantuan)
        menuTentang = findViewById(R.id.menuTentang)
        btnLogoutUser = findViewById(R.id.btnLogoutUser)
    }

    private fun showCropRotateDialog(imageUri: Uri) {
        val bitmap = decodeSampledBitmapFromUri(imageUri, 1280, 1280)
        if (bitmap == null) {
            Toast.makeText(this, "Gagal memuat gambar yang dipilih", Toast.LENGTH_SHORT).show()
            return
        }

        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_crop_rotate_photo)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.94).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(false)

        val cropRotateView = dialog.findViewById<CropRotateImageView>(R.id.cropRotateView)
        val btnRotateLeft = dialog.findViewById<Button>(R.id.btnRotateLeft)
        val btnRotateRight = dialog.findViewById<Button>(R.id.btnRotateRight)
        val btnResetCrop = dialog.findViewById<Button>(R.id.btnResetCrop)
        val seekBarRotation = dialog.findViewById<SeekBar>(R.id.seekBarRotation)
        val tvRotationDegree = dialog.findViewById<TextView>(R.id.tvRotationDegree)
        val seekBarZoom = dialog.findViewById<SeekBar>(R.id.seekBarZoom)
        val tvZoomPercent = dialog.findViewById<TextView>(R.id.tvZoomPercent)
        val btnCancelCrop = dialog.findViewById<Button>(R.id.btnCancelCrop)
        val btnSaveCrop = dialog.findViewById<Button>(R.id.btnSaveCrop)

        cropRotateView.setImageBitmap(bitmap)

        fun syncRotationUI() {
            val deg = cropRotateView.getRotationAngle().roundToInt()
            tvRotationDegree.text = "${deg}°"
            seekBarRotation.progress = (deg + 180).coerceIn(0, 360)
        }

        cropRotateView.onZoomChanged = { zoom ->
            val progress = ((zoom - 0.5f) * 100f).roundToInt().coerceIn(0, 350)
            seekBarZoom.progress = progress
            tvZoomPercent.text = "${(zoom * 100).roundToInt()}%"
        }

        btnRotateLeft.setOnClickListener {
            cropRotateView.rotateBy(-90f)
            syncRotationUI()
        }

        btnRotateRight.setOnClickListener {
            cropRotateView.rotateBy(90f)
            syncRotationUI()
        }

        btnResetCrop.setOnClickListener {
            cropRotateView.resetTransform()
            seekBarRotation.progress = 180
            tvRotationDegree.text = "0°"
            seekBarZoom.progress = 50
            tvZoomPercent.text = "100%"
        }

        seekBarRotation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val angle = (progress - 180).toFloat()
                    cropRotateView.setRotationAngle(angle)
                    tvRotationDegree.text = "${angle.roundToInt()}°"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarZoom.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val zoom = 0.5f + (progress / 100f)
                    cropRotateView.setZoomRatio(zoom)
                    tvZoomPercent.text = "${(zoom * 100).roundToInt()}%"
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnCancelCrop.setOnClickListener {
            dialog.dismiss()
        }

        btnSaveCrop.setOnClickListener {
            val croppedBitmap = cropRotateView.getCroppedBitmap(512)
            if (croppedBitmap != null) {
                val localUri = saveBitmapToInternalStorage(croppedBitmap)
                if (localUri != null) {
                    applyAndPersistProfilePhoto(localUri)
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun decodeSampledBitmapFromUri(uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }

            var inSampleSize = 1
            if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply { this.inSampleSize = inSampleSize }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOptions) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun applyAndPersistProfilePhoto(localUri: Uri) {
        val photoUriWithTimestamp = "$localUri?t=${System.currentTimeMillis()}"
        ivProfileAvatar.setPadding(0, 0, 0, 0)
        Glide.with(this)
            .load(localUri)
            .circleCrop()
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(ivProfileAvatar)

        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val email = (sharedPreferences.getString("KEY_EMAIL", "default") ?: "default").trim().lowercase()
        sharedPreferences.edit()
            .putString("KEY_PROFILE_IMAGE", localUri.toString())
            .apply()

        val permanentPrefs = getSharedPreferences("PermanentUserPrefs", MODE_PRIVATE)
        permanentPrefs.edit()
            .putString("PROFILE_IMAGE_$email", localUri.toString())
            .putString("PROFILE_IMAGE_LAST", localUri.toString())
            .apply()

        updateProfilePictureOnServer(photoUriWithTimestamp)
    }

    private fun displayUserInfo() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val permanentPrefs = getSharedPreferences("PermanentUserPrefs", MODE_PRIVATE)

        val name = sharedPreferences.getString("KEY_NAME", "Warga Pelapor") ?: "Warga Pelapor"
        val email = sharedPreferences.getString("KEY_EMAIL", null)
            ?: sharedPreferences.getString("KEY_SAVED_IDENTIFIER", "warga@pinpoint.id")
            ?: "warga@pinpoint.id"
        val emailKey = email.trim().lowercase()

        tvProfileName.text = name
        tvProfileEmail.text = email

        val permanentPhoto = permanentPrefs.getString("PROFILE_IMAGE_$emailKey", null)
            ?: permanentPrefs.getString("PROFILE_IMAGE_LAST", null)
        val sessionPhoto = sharedPreferences.getString("KEY_PROFILE_IMAGE", null)

        val localFile = getProfileImageFile(emailKey)
        val resolvedPhotoPath = when {
            localFile.exists() -> Uri.fromFile(localFile).toString()
            !permanentPhoto.isNullOrEmpty() -> permanentPhoto
            !sessionPhoto.isNullOrEmpty() -> sessionPhoto
            else -> null
        }

        if (!resolvedPhotoPath.isNullOrEmpty()) {
            ivProfileAvatar.setPadding(0, 0, 0, 0)
            Glide.with(this)
                .load(resolvedPhotoPath)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .error(R.drawable.user)
                .into(ivProfileAvatar)
        }
    }

    private fun getProfileImageFile(emailKey: String): File {
        val safeEmail = emailKey.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return File(filesDir, "profile_picture_$safeEmail.jpg")
    }

    private fun saveBitmapToInternalStorage(bitmap: Bitmap): Uri? {
        return try {
            val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
            val email = (sharedPreferences.getString("KEY_EMAIL", "default") ?: "default").trim().lowercase()

            val file = getProfileImageFile(email)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.flush()
            }

            val fallbackFile = File(filesDir, "profile_picture_user.jpg")
            FileOutputStream(fallbackFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                out.flush()
            }

            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateProfilePictureOnServer(photoUrl: String) {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        val rawToken = sharedPreferences.getString("KEY_TOKEN", "") ?: ""
        val token = if (rawToken.startsWith("Bearer ")) rawToken else "Bearer $rawToken"
        val name = sharedPreferences.getString("KEY_NAME", "") ?: ""
        val email = sharedPreferences.getString("KEY_EMAIL", "") ?: ""

        val updateReq = UserAccountDto(
            fullName = name,
            email = email,
            photoUrl = photoUrl
        )

        ApiClient.fetchWithFallback(
            callFactory = { api -> api.updateProfile(token, updateReq) },
            onResponse = { _, response ->
                if (response.isSuccessful) {
                    Toast.makeText(this@ProfilActivity, "Foto profil berhasil dipotong & disimpan!", Toast.LENGTH_SHORT).show()
                }
            },
            onFailure = { _, _ ->
                Toast.makeText(this@ProfilActivity, "Foto profil berhasil disimpan di perangkat!", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupMenuClickListeners() {
        val openPhotoPicker = {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }
        btnEditPhoto.setOnClickListener { openPhotoPicker() }
        ivProfileAvatar.setOnClickListener { openPhotoPicker() }

        menuBiodata.setOnClickListener {
            startActivity(Intent(this, BiodataActivity::class.java))
        }
        menuKeamanan.setOnClickListener {
            startActivity(Intent(this, KeamananAkunActivity::class.java))
        }
        menuBantuan.setOnClickListener {
            startActivity(Intent(this, BantuanActivity::class.java))
        }
        menuTentang.setOnClickListener {
            startActivity(Intent(this, TentangActivity::class.java))
        }
    }

    private fun performLogout() {
        val sharedPreferences = getSharedPreferences("UserSession", MODE_PRIVATE)
        sharedPreferences.edit()
            .putBoolean("KEY_IS_LOGGED_IN", false)
            .remove("KEY_TOKEN")
            .apply()

        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupBottomNavigation() {
        com.example.pinpoint.admin.NavbarUtils.setupUserNavbar(this)
    }
}
