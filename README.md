# TvBoxDemoPlugin
[TvBox](https://github.com/muedsa/TvBox)的demo插件

## Use this template(使用此仓库作为模板)
本仓库使用**git submodule**,请在项目Clone后使用`git submodule update --init --recursive`拉取子模块。  
你需要修改以下位置
- [ ] [settings.gradle.kts](settings.gradle.kts) 中的 `rootProject.name = "你的项目名称"`
- [ ] [app/src/main/res/values/strings.xml](app/src/main/res/values/strings.xml) 中的 `<string name="app_name">你的插件名称</string>`
- [ ] [app/build.gradle.kts](app/build.gradle.kts) 中的 `namespace = "你的namespace"`
- [ ] [app/build.gradle.kts](app/build.gradle.kts) 中的 `applicationId = "applicationId"`
- [ ] [app/build.gradle.kts](app/build.gradle.kts) 中的 `signingConfigs { // 你的签名 }`
- [ ] [app/src/main/res/mipmap-xxxx](app/src/main/res) 中的 [ic_launcher](app/src/main/res/mipmap-hdpi/ic_launcher.webp) 为你的Icon
- [ ] 编写代码实现插件IPlugin的所有功能,并修改 [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) 中的 `<meta-data android:name="tv_box_plugin_entry_point_impl" android:value="你的插件packageName.ClassName"/>`
- [ ] [README.md](README.md)
