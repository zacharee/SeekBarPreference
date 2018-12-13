package tk.zwander.sample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content, SliderFragment())
            .commit()
    }

    class SliderFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.prefs_slider, rootKey)
        }
    }
}
