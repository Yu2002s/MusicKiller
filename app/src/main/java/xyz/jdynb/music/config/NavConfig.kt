package xyz.jdynb.music.config

import androidx.navigation.NavDestination

//private val hideToolbarDestinations = setOf<Int>(
//  // R.id.homeFragment
//)

private val hideBottomNavDestinations = setOf<Int>(
  // R.id.homeFragment
)

//fun NavDestination.shouldShowToolbar(): Boolean {
//  return this.id !in hideToolbarDestinations
//}

/**
 * 目标导航能否显示底栏
 */
fun NavDestination.shouldShowBottomNav(): Boolean {
  return this.id !in hideBottomNavDestinations
}