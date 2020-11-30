# libads
# Setup
1. Clone Modul quảng cáo về cùng thư mục với App
2. Thêm modul vào Project
  - file Setting.gradle thêm
    include ':Ads'
    project(':Ads').projectDir = new File(settingsDir, '../LibAds/Ads')
  
  - file build.gradle thêm
    implementation project(path: ':Ads')

  

# Init
Phải gọi init đầu tiên trước khi gọi những hàm khác
