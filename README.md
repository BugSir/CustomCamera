# CustomCamera 简单的自定义拍照（抽离zxingdemo的相机管理进行修改）
[![](https://jitpack.io/v/BugSir/CustomCamera.svg)](https://jitpack.io/#BugSir/CustomCamera)
# 引用方法:<br/>
<pre><code>
工程目录gradle
	allprojects {
		repositories {
			maven { url 'https://jitpack.io' }
		}
	}
项目gradle
	dependencies {
	        implementation 'com.github.BugSir:CustomCamera:1.0.0'
	}
</code></pre>
# 示例
![示例图片](https://github.com/BugSir/CustomCamera/blob/master/app/image/image.gif)
# 使用：<br/>
#### oncreate初始化：
``` java
CameraManagerUtil.with(this)
.setCropView(cropView)//截取区域view
.setSurfaceView(surfaceView)//
.setCameraCallback(this)//相机回调
.setBeepResId(resId)//拍照声音（不调用此方法就不会调用播放声音）
.setHasStatusBar(true);//是否要扣掉状态栏
```
#### 然后在onresume(),onWindowFocusChanged(),onPause(),onDestroy()实现相应的方法。
``` java
@Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        mCameraUtil.onWindowFocusChanged();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)== PackageManager.PERMISSION_GRANTED)
        {
            mCameraUtil.onResume();
        }else
        {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},888);
        }

    }
@Override
    protected void onPause() {
        mCameraUtil.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mCameraUtil.onDestory();
        mCameraUtil=null;
        super.onDestroy();
    }
```
### 拍照：
#### 实现接口
``` java
public interface ICameraCallback {
    void cameraError();//相机调用出错会回调回来，在这里做提示，提醒用户
    void takePhotoSuccess();//拍照成功回调
}
```
#### 开始拍照
```java
mCameraUtil.takePhoto();//成功会回调takePhotoSuccess（）
```
#### 获取拍照结果
```java
Bitmap bitmap=mCameraUtil.getPhotoResult();//此方法会截取你限定范围内的照片内容，故最好放在异步线程里操作
```
