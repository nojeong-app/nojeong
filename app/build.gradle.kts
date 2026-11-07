import java.util.Properties
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties().also { p ->
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { p.load(it) }
}
fun key(name: String, default: String = "") = "\"" + (localProps.getProperty(name) ?: default) + "\""

android {
    namespace = "kr.tripbet.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "kr.tripbet.app"
        minSdk = 28          // Pleos 요구: API 28 이상
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
        // 키는 local.properties 에서만 읽는다(저장소 제외). 값은 코드·채팅에 쓰지 않는다.

    }
    // 서명 키 정보는 프로젝트 맨 위 폴더의 keystore.properties(저장소에 올리지 않음)에서만 읽는다. 없으면 서명하지 않는다.
    // 항목: storeFile, storePassword, keyAlias, keyPassword
    val ksFile = rootProject.file("keystore.properties")
    if (ksFile.exists()) {
        val ksp = Properties().also { p -> ksFile.inputStream().use { p.load(it) } }
        signingConfigs {
            create("submit") {
                storeFile = file(ksp.getProperty("storeFile")); storePassword = ksp.getProperty("storePassword")
                keyAlias = ksp.getProperty("keyAlias"); keyPassword = ksp.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        // 개발용(debug): 키를 넣어 실시간 호출. 제출용(release): 키를 넣지 않고 assets 의 미리 담은 데이터만 쓴다.
        debug {
            buildConfigField("boolean", "LIVE_API", "true")
            buildConfigField("String", "DATA_GO_KR_KEY", key("DATA_GO_KR_KEY"))   // 관광공사·환경공단 공용
            buildConfigField("String", "EX_API_KEY", key("EX_API_KEY", "test"))   // 도로공사, 시연은 test
        }
        release {
            signingConfigs.findByName("submit")?.let { signingConfig = it }
            buildConfigField("boolean", "LIVE_API", "false")
            buildConfigField("String", "DATA_GO_KR_KEY", "\"\"")
            buildConfigField("String", "EX_API_KEY", "\"\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")

    // Pleos Connect SDK (공개 Maven). 사용 신청·권한은 Pleos 가입 후 확인.
    // 아직 코드에서 호출하지 않음 — 뼈대 단계. 버전은 저장소 maven-metadata.xml 기준 최신.
    implementation("ai.pleos.playground:Vehicle:2.3.2")
    implementation("ai.pleos.playground:NaviHelper:3.0.5")
    implementation("ai.pleos.playground:TextToSpeech:3.3.1")
    implementation("ai.pleos.playground:SpeechToText:3.3.1")
}





