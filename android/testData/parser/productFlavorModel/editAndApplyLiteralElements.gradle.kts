android {
  defaultConfig {
    applicationId("com.example.myapplication")
    dimension("abcd")
    maxSdkVersion(23)
    minSdkVersion("15")
    multiDexEnabled(true)
    targetSdkVersion("22")
    testApplicationId("com.example.myapplication.test")
    testFunctionalTest(false)
    testHandleProfiling(true)
    testInstrumentationRunner("abcd")
    useJack(false)
    versionCode(1)
    versionName("1.0")
  }
}