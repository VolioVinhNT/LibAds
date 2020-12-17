# libads
## Setup
1. Clone Modul quảng cáo về cùng thư mục với App
2. Thêm modul vào Project
  - file Setting.gradle thêm
  
    include ':Ads'
    
    project(':Ads').projectDir = new File(settingsDir, '../LibAds/Ads')
  
  
  - file build.gradle thêm
  
    implementation project(path: ':Ads')

3. File AndroidManifest
 Thêm:
     ```xml
     <meta-data
       android:name="com.google.android.gms.ads.APPLICATION_ID"
       android:value="@string/admob_app_id" />
     ```

     (admob_app_id xin bên PO)

## Init
Phải gọi init đầu tiên trước khi gọi những hàm khác

```java
AdsController.init(
            activity: Activity,
            isDebug:Boolean,
            listAppId: ArrayList<String>,
            packetName: String,
            listPathJson: ArrayList<String>, lifecycle: Lifecycle
        )
 ```
 
|Parameters| Giá trị|
|------|-------|
|activity| Activity|
|isDebug| Giá trị bằng true sẽ sử dụng id quảng cáo test, bằng false <br>sử dụng id thật chỉ sử dụng khi build release |
|listAppId| Là một list String id của mạng quảng cáo được thêm trong AndroidManifest admob_app_id |
|packetName| Là packet của app lấy trong file build.gradle (modul app) => applicationId  |
|listPathJson| Là list path json của mạng quảng cáo (file json nằm trong assets)<br>vd: arrayListOf("admod_json/admod_id.json")|
|lifecycle | Là lifecycle của activity|

## Sử dụng
Ở vị trị cần hiển thị quảng cáo gọi

```java
AdsController.loadAndShow(
        spaceName: String,
        isKeepAds: Boolean = false,
        loadingText: String? = null,
        layout: ViewGroup? = null,
        layoutAds: View? = null,
        lifecycle: Lifecycle? = null,
        timeMillisecond: Long? = null,
        adCallback: AdCallback? = null
    )
 ```

|Parameters| Giá trị|
|------|-------|
|activity| Activity|
|spaceName| Là tên vị trí gắn quảng cáo tướng ứng với spaceName trong file json bên PO gửi |
|isKeepAds| Biến sác định có tiếp tục giữ lại quảng cáo khi bị timeout để hiểng thị lại hay không |
|layout| Là một ViewGroup để chứa quảng cáo (cần có khi sử dụng quảng cáo Banner và  Native) |
|layoutAds| Là View quy định bố cục hiển thị quảng cáo Native)|
|lifecycle | Là lifecycle của activity hoặc fragment |
|timeMillisecond | Là thời gian timeout load quảng cáo tính bằng miliseconds|
|adCallback | Callback trả về trạng thái của quảng cáo|

```java
AdsController.preload(spaceName: String)
```
Load trước quảng cáo với spaceName trong file json




```java
AdsController.show(
        spaceName: String,
        textLoading: String? = null,
        layout: ViewGroup? = null,
        layoutAds: View? = null,
        lifecycle: Lifecycle? = null,
        timeMillisecond: Long ,
        adCallback: AdCallback? =null
    )
```
    
  Hiển thị các quảng cáo được load từ trước, khi vị trí spaceName được load trước bị lỗi sẽ load lại và show quảng cáo mới
  các Parameters tương tự loadAndShow




  - Quảng cáo OpenApp,Interstitial,Reward cần gọi:
```java

    AdsController.getInstance().loadAndShow(
                  spaceName,
                  textLoading,
                  timeMillisecond,
                  lifecycle ,
                  adCallback )
 ```               
   - Quảng cáo Banner,AdaptiveBanner gọi:
  ```java 
    AdsController.loadAndShow(
          spaceName,
          layout: ViewGroup,
          adCallback: AdCallback
      )
  ```
  - Quảng cáo Native gọi:
   ```java 
    AdsController.loadAndShow(
          spaceName,
          layout: ViewGroup,
          layoutAds: View,
          adCallback: AdCallback
      )
  ```
 với layoutAds = LayoutInflater.from(context).inflate(R.layout.layout_ads,null)
 
 Mẫu file xml:

 ```xml
  <androidx.constraintlayout.widget.ConstraintLayout
        xmlns:android="http://schemas.android.com/apk/res/android"
        xmlns:app="http://schemas.android.com/apk/res-auto"
        xmlns:tools="http://schemas.android.com/tools"
        android:layout_width="match_parent"
        android:layout_height="300dp"
        android:layout_gravity="center"
        android:orientation="vertical"
        >

        <FrameLayout
            android:id="@+id/ad_media"
            android:layout_width="0dp"
            android:layout_height="0dp"
            android:layout_gravity="center_horizontal"
            android:layout_marginBottom="5dp"
            android:visibility="visible"
            app:layout_constraintBottom_toTopOf="@+id/layout_middle"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />

        <TextView
            android:id="@+id/tv_sponsored"
            android:layout_width="wrap_content"
            android:layout_height="wrap_content"
            android:layout_marginTop="4dp"
            android:layout_marginEnd="24dp"
            android:text="Sponsored"
            android:textColor="?attr/colorText"
            android:textSize="7sp"
            app:layout_constraintEnd_toEndOf="parent"
            app:layout_constraintTop_toTopOf="parent"
            android:layout_marginRight="24dp" />
        <TextView
            android:id="@+id/ic_ad"
            android:layout_width="22dp"
            android:layout_height="15dp"
            android:background="#F44336"
            android:gravity="center"
            android:text="ADS"
            android:textColor="@android:color/white"
            android:textSize="10sp"
            android:textStyle="bold"
            app:layout_constraintStart_toStartOf="parent"
            app:layout_constraintTop_toTopOf="parent" />
        <com.github.florent37.shapeofview.shapes.CircleView
            android:id="@+id/layout_icon"
            android:layout_width="0dp"
            android:layout_height="0dp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintDimensionRatio="1:1"
            app:layout_constraintHeight_percent="0.6"
            app:layout_constraintLeft_toLeftOf="parent"
            android:layout_marginLeft="12dp"
            app:layout_constraintTop_toTopOf="parent"
            >
            <ImageView
                android:id="@+id/ad_app_icon"
                android:layout_width="match_parent"
                android:layout_height="match_parent"

                android:adjustViewBounds="true"
                 />
        </com.github.florent37.shapeofview.shapes.CircleView>


        <TextView
            android:id="@+id/ad_headline"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginHorizontal="@dimen/_10sdp"
            android:layout_marginStart="10dp"
            android:layout_marginTop="4dp"
            android:ellipsize="end"
            android:maxLines="1"
            android:textColor="#FEC502"
            android:textSize="12sp"

            android:textStyle="bold"
            app:layout_constraintEnd_toStartOf="@+id/ad_call_to_action"
            app:layout_constraintLeft_toRightOf="@+id/ad_call_to_action"
            app:layout_constraintRight_toLeftOf="@+id/ad_call_to_action"
            app:layout_constraintStart_toEndOf="@+id/layout_icon"
            app:layout_constraintTop_toTopOf="parent"
            android:layout_marginLeft="10dp" />

        <TextView
            android:id="@+id/ad_body"

            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_marginHorizontal="@dimen/_10sdp"
            android:layout_marginTop="4dp"
            android:layout_marginBottom="8dp"
            android:textColor="?attr/colorText"
            android:ellipsize="end"
            android:maxLines="2"
            android:textSize="11sp"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintEnd_toStartOf="@+id/ad_call_to_action"
            app:layout_constraintLeft_toRightOf="@+id/ad_call_to_action"
            app:layout_constraintRight_toLeftOf="@+id/ad_call_to_action"
            app:layout_constraintStart_toEndOf="@+id/layout_icon"
            app:layout_constraintTop_toBottomOf="@+id/ad_headline" />

        <Button
            android:id="@+id/ad_call_to_action"
            android:layout_width="wrap_content"
            android:layout_height="0dp"
            android:layout_marginRight="8dp"
            android:layout_marginBottom="4dp"
            android:background="@drawable/bg_admob_cta"
            android:backgroundTint="#63C6EF"
            android:gravity="center"
            android:textColor="#fff"
            android:textSize="12sp"
            android:textStyle="bold"
            app:layout_constraintBottom_toBottomOf="parent"
            app:layout_constraintDimensionRatio="2.5:1"
            app:layout_constraintRight_toRightOf="parent" />

    </androidx.constraintlayout.widget.ConstraintLayout>
```

Các id buộc phải trùng id dưới bảng :

|Id|View và tác dụng|
|---|-------|
|ad_media|Là ViewGroup chứa view hiển thị video,ảnh lớn|
|ad_app_icon| Là ImageView hiển thị icon quảng cáo|
|ad_headline| Là TextView hiển thị title quảng cáo|
|ad_body| Là TextView hiển thị nội dung quảng cáo|
|ad_call_to_action|Button quảng cáo|

