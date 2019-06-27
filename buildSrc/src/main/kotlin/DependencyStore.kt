/*
 * Copyright (c) 2016-2019 Projekt Substratum
 * This file is part of Substratum.
 *
 * SPDX-License-Identifier: GPL-3.0-Or-Later
 */
class DependencyStore {

    object AndroidX {
        private const val annotationVersion = "1.1.0"
        private const val apkSigVersion = "3.4.0"
        private const val appcompatVersion = "1.1.0-beta01"
        private const val cardViewVersion = "1.0.0"
        private const val constraintlayoutVersion = "2.0.0-beta2"
        private const val coreKtxVersion = "1.2.0-alpha02"
        private const val databindingVersion = "3.5.0-beta04"
        private const val fragmentKtxVersion = "1.1.0-beta01"
        private const val paletteVersion = "1.0.0"
        private const val preferenceVersion = "1.1.0-beta01"
        private const val slicesVersion = "1.1.0-alpha01"
        private const val slicesKtxVersion = "1.0.0-alpha07"

        const val annotations = "androidx.annotation:annotation:$annotationVersion"
        const val apksig = "com.android.tools.build:apksig:$apkSigVersion"
        const val appcompat = "androidx.appcompat:appcompat:$appcompatVersion"
        const val cardview = "androidx.cardview:cardview:$cardViewVersion"
        const val constraintlayout = "androidx.constraintlayout:constraintlayout:$constraintlayoutVersion"
        const val coreKtx = "androidx.core:core-ktx:$coreKtxVersion"
        const val databindingAdapters = "androidx.databinding:databinding-adapters:$databindingVersion"
        const val databindingRuntime = "androidx.databinding:databinding-runtime:$databindingVersion"
        const val fragmentKtx = "androidx.fragment:fragment-ktx:$fragmentKtxVersion"
        const val palette = "androidx.palette:palette:$paletteVersion"
        const val preference = "androidx.preference:preference:$preferenceVersion"
        const val sliceBuilders = "androidx.slice:slice-builders:$slicesVersion"
        const val sliceCore = "androidx.slice:slice-core:$slicesVersion"
        const val sliceKtx = "androidx.slice:slice-builders-ktx:$slicesKtxVersion"
    }

    object Debugging {
        private const val leakcanaryVersion = "2.0-alpha-2"

        const val leakcanary = "com.squareup.leakcanary:leakcanary-android:$leakcanaryVersion"
    }

    object Material {
        private const val materialVersion = "1.1.0-alpha07"

        const val material = "com.google.android.material:material:$materialVersion"
    }

    object ThirdParty {
        private const val aboutVersion = "7.0.0"
        private const val caocVersion = "2.2.0"
        private const val commonsIoVersion = "2.6"
        private const val expandableLayoutVersion = "2.9.2"
        private const val fabSheetVersion = "1.2.1"
        private const val floatingHeadVersion = "2.4.4"
        private const val gestureRecyclerVersion = "1.8.0"
        private const val glideVersion = "4.9.0"
        private const val imageCropperVersion = "2.8.0"
        private const val svgViewVersion = "1.0.6"
        private const val welcomeVersion = "1.4.1"
        private const val ztZipVersion = "1.13"

        const val aboutlibraries = "com.mikepenz:aboutlibraries:$aboutVersion@aar"
        const val coac = "cat.ereza:customactivityoncrash:$caocVersion"
        const val commonsio = "commons-io:commons-io:$commonsIoVersion"
        const val expandablelayout = "net.cachapa.expandablelayout:expandablelayout:$expandableLayoutVersion"
        const val floatingview = "com.github.recruit-lifestyle:FloatingView:$floatingHeadVersion"
        const val gesturerecycler = "com.thesurix.gesturerecycler:gesture-recycler:$gestureRecyclerVersion"
        const val glide = "com.github.bumptech.glide:glide:$glideVersion"
        const val glideCompiler = "com.github.bumptech.glide:compiler:$glideVersion"
        const val imagecropper = "com.theartofdev.edmodo:android-image-cropper:$imageCropperVersion"
        const val sheetfab = "com.gordonwong:material-sheet-fab:$fabSheetVersion"
        const val svgview = "com.jaredrummler:animated-svg-view:$svgViewVersion"
        const val welcome = "com.stephentuso:welcome:$welcomeVersion"
        const val ztzip = "org.zeroturnaround:zt-zip:$ztZipVersion"
    }
}
