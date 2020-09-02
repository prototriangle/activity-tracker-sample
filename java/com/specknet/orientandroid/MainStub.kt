package com.specknet.orientandroid

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.navigateUp
import kotlinx.android.synthetic.main.main_stub_activity.*

class MainStub : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_stub_activity)
        setSupportActionBar(main_stub_toolbar)
        navController = findNavController(R.id.nav_host_fragment)
        appBarConfiguration = AppBarConfiguration(navController.graph)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_navigation, menu)
        NavigationUI.setupWithNavController(main_stub_toolbar, navController, appBarConfiguration)
        return true
    }

    fun setMenu(id: Int) {
        if (id == 0) {
            Log.e("setMenu", "New menu id is 0. Cannot set menu")
            return
        }
        main_stub_toolbar.menu.clear()
        main_stub_toolbar.inflateMenu(id)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return item.onNavDestinationSelected(navController, null) || super.onOptionsItemSelected(item)
    }

    private fun MenuItem.onNavDestinationSelected(navController: NavController, args: Bundle?): Boolean {
        val builder = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setEnterAnim(R.anim.nav_default_enter_anim)
                .setExitAnim(R.anim.slide_out_left)
                .setPopEnterAnim(R.anim.slide_in_left)
                .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
        val options = builder.build()
        return try {
            navController.navigate(this.itemId, args, options)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

}
