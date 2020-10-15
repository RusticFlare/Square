package io.github.rusticflare.square

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.graphics.applyCanvas
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileOutputStream


class MainActivity : AppCompatActivity() {

    private var currentImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            //permission denied
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            );
            //show popup to request runtime permission
            requestPermissions(permissions, PERMISSION_CODE);
        }

        setContentView(R.layout.activity_main)

        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            handleImage(intent.clipData?.getItemAt(0)?.uri!!)
        }

        open.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED ||
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
                //permission denied
                val permissions = arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                );
                //show popup to request runtime permission
                requestPermissions(permissions, PERMISSION_CODE);
            } else {
                //permission already granted
                pickImageFromGallery();
            }
        }

        share.setOnClickListener {
            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, currentImageUri)
                type = "image/jpeg"
            }
            startActivity(Intent.createChooser(shareIntent, resources.getText(R.string.send_to)))
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun pickImageFromGallery() {
        //Intent to pick image
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    companion object {
        //image pick code
        private val IMAGE_PICK_CODE = 1000;
        //Permission code
        private val PERMISSION_CODE = 1001;
    }

    //handle requested permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when(requestCode){
            PERMISSION_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    //permission from popup granted
                    pickImageFromGallery()
                } else {
                    //permission from popup denied
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //handle result of picked image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE) {
            handleImage(data?.data!!)
        }
    }

    private fun handleImage(uri: Uri) {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, options)
        }

        val width = options.outWidth
        val height = options.outHeight

        val (newWidth, newHeight) = if (width <= 1080 && height <= 1080) {
            width to height
        } else if (width > height) {
            1080 to (1080 * (height.toDouble() / width)).toInt()
        } else {
            (1080 * (width.toDouble() / height)).toInt() to 1080
        }

        val imageBitmap = contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it)
        }!!

        val resized = Bitmap.createScaledBitmap(imageBitmap, newWidth, newHeight, true)

        val bitmap = Bitmap.createBitmap(1080, 1080, Bitmap.Config.ARGB_8888)
            .applyCanvas {
                drawColor(Color.WHITE)
                drawBitmap(resized, (1080f - newWidth) / 2, (1080f - newHeight) / 2, null)
            }

        val tempFile = File.createTempFile("squared", ".jpg", cacheDir)
        FileOutputStream(tempFile).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }
        currentImageUri = bitmap.externalUri()
        image_view.setImageURI(currentImageUri)
    }

    private fun Bitmap.externalUri(): Uri {
        val file = File.createTempFile("squared", ".jpg", externalCacheDir)
        FileOutputStream(file).use {
            compress(Bitmap.CompressFormat.JPEG, 100, it)
            it.flush()
        }
        return FileProvider.getUriForFile(
            this@MainActivity,
            applicationContext.packageName + ".provider",
            file
        );
    }
}

