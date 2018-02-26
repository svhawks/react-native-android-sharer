
# react-native-android-sharer

## Getting started

`$ npm install react-native-android-sharer --save`

### Mostly automatic installation

`$ react-native link react-native-android-sharer`

### Manual installation


#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.svtek.reactnative.RNAndroidSharerPackage;` to the imports at the top of the file
  - Add `new RNAndroidSharerPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-android-sharer'
  	project(':react-native-android-sharer').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-android-sharer/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-android-sharer')
  	```


## Usage
```javascript
import RNAndroidSharer from 'react-native-android-sharer';

// TODO: What to do with the module?
RNAndroidSharer;
```
  