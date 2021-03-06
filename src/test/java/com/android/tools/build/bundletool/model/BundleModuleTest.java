/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.tools.build.bundletool.model;

import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.androidManifest;
import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.withFeatureCondition;
import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.withFusingAttribute;
import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.withMinSdkCondition;
import static com.android.tools.build.bundletool.testing.ManifestProtoUtils.withUsesSplit;
import static com.android.tools.build.bundletool.testing.TargetingUtils.mergeModuleTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.moduleFeatureTargeting;
import static com.android.tools.build.bundletool.testing.TargetingUtils.moduleMinSdkVersionTargeting;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.google.common.truth.extensions.proto.ProtoTruth.assertThat;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.android.aapt.Resources.Package;
import com.android.aapt.Resources.ResourceTable;
import com.android.aapt.Resources.XmlNode;
import com.android.bundle.Config.BundleConfig;
import com.android.bundle.Files.Assets;
import com.android.bundle.Files.NativeLibraries;
import com.android.bundle.Files.TargetedAssetsDirectory;
import com.android.bundle.Files.TargetedNativeDirectory;
import com.android.tools.build.bundletool.testing.BundleConfigBuilder;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class BundleModuleTest {

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  private static final BundleConfig DEFAULT_BUNDLE_CONFIG = BundleConfigBuilder.create().build();

  @Test
  public void missingAssetsProtoFile_returnsEmptyProto() {
    BundleModule bundleModule = createMinimalModuleBuilder().build();

    assertThat(bundleModule.getAssetsConfig()).isEmpty();
  }

  @Test
  public void correctAssetsProtoFile_parsedAndReturned() throws Exception {
    Assets assetsConfig =
        Assets.newBuilder()
            .addDirectory(TargetedAssetsDirectory.newBuilder().setPath("assets/data-armv6"))
            .build();

    BundleModule bundleModule =
        createMinimalModuleBuilder()
            .addEntry(InMemoryModuleEntry.ofFile("assets.pb", assetsConfig.toByteArray()))
            .build();

    assertThat(bundleModule.getAssetsConfig()).hasValue(assetsConfig);
  }

  @Test
  public void incorrectAssetsProtoFile_throws() throws Exception {
    byte[] badAssetsFile = new byte[] {'b', 'a', 'd'};

    assertThrows(
        IOException.class,
        () ->
            createMinimalModuleBuilder()
                .addEntry(InMemoryModuleEntry.ofFile("assets.pb", badAssetsFile)));
  }

  @Test
  public void missingNativeProtoFile_returnsEmptyProto() {
    BundleModule bundleModule = createMinimalModuleBuilder().build();

    assertThat(bundleModule.getNativeConfig()).isEmpty();
  }

  @Test
  public void correctNativeProtoFile_parsedAndReturned() throws Exception {
    NativeLibraries nativeConfig =
        NativeLibraries.newBuilder()
            .addDirectory(TargetedNativeDirectory.newBuilder().setPath("native/x86"))
            .build();

    BundleModule bundleModule =
        createMinimalModuleBuilder()
            .addEntry(InMemoryModuleEntry.ofFile("native.pb", nativeConfig.toByteArray()))
            .build();

    assertThat(bundleModule.getNativeConfig()).hasValue(nativeConfig);
  }

  @Test
  public void incorrectNativeProtoFile_throws() throws Exception {
    byte[] badNativeFile = new byte[] {'b', 'a', 'd'};

    assertThrows(
        IOException.class,
        () ->
            createMinimalModuleBuilder()
                .addEntry(InMemoryModuleEntry.ofFile("native.pb", badNativeFile)));
  }

  @Test
  public void missingResourceTableProtoFile_returnsEmptyProto() throws Exception {
    BundleModule bundleModule = createMinimalModuleBuilder().build();

    assertThat(bundleModule.getResourceTable()).isEmpty();
  }

  @Test
  public void correctResourceTableProtoFile_parsedAndReturned() throws Exception {
    ResourceTable resourceTable =
        ResourceTable.newBuilder().addPackage(Package.getDefaultInstance()).build();

    BundleModule bundleModule =
        createMinimalModuleBuilder()
            .addEntry(InMemoryModuleEntry.ofFile("resources.pb", resourceTable.toByteArray()))
            .build();

    assertThat(bundleModule.getResourceTable()).hasValue(resourceTable);
  }

  @Test
  public void incorrectResourceTable_throws() throws Exception {
    byte[] badResourcesFile = new byte[] {'b', 'a', 'd'};

    assertThrows(
        IOException.class,
        () ->
            createMinimalModuleBuilder()
                .addEntry(InMemoryModuleEntry.ofFile("resources.pb", badResourcesFile)));
  }

  @Test
  public void missingProtoManifestFile_throws() {
    BundleModule.Builder minimalModuleWithoutManifest =
        BundleModule.builder()
            .setName(BundleModuleName.create("testModule"))
            .setBundleConfig(DEFAULT_BUNDLE_CONFIG);

    IllegalStateException exception =
        assertThrows(IllegalStateException.class, () -> minimalModuleWithoutManifest.build());

    assertThat(exception).hasMessageThat().contains("Missing required properties: androidManifest");
  }

  @Test
  public void correctProtoManifestFile_parsedAndReturned() throws Exception {
    XmlNode manifestXml = androidManifest("com.test.app");

    BundleModule bundleModule =
        BundleModule.builder()
            .setName(BundleModuleName.create("testModule"))
            .setBundleConfig(DEFAULT_BUNDLE_CONFIG)
            .addEntry(
                InMemoryModuleEntry.ofFile(
                    "manifest/AndroidManifest.xml", manifestXml.toByteArray()))
            .build();

    assertThat(bundleModule.getAndroidManifest().getManifestRoot().getProto())
        .isEqualTo(manifestXml);
  }

  @Test
  public void incorrectProtoManifest_throws() throws Exception {
    byte[] badManifestFile = new byte[] {'b', 'a', 'd'};
    BundleModule.Builder minimalModuleWithoutManifest =
        BundleModule.builder()
            .setName(BundleModuleName.create("testModule"));

    assertThrows(
        IOException.class,
        () ->
            minimalModuleWithoutManifest.addEntry(
                InMemoryModuleEntry.ofFile("manifest/AndroidManifest.xml", badManifestFile)));
  }

  @Test
  public void specialFiles_areNotStoredAsEntries() throws Exception {
    BundleModule bundleModule =
        BundleModule.builder()
            .setName(BundleModuleName.create("testModule"))
            .setBundleConfig(DEFAULT_BUNDLE_CONFIG)
            .addEntry(
                InMemoryModuleEntry.ofFile(
                    "manifest/AndroidManifest.xml", androidManifest("com.test.app").toByteArray()))
            .addEntry(
                InMemoryModuleEntry.ofFile("assets.pb", Assets.getDefaultInstance().toByteArray()))
            .addEntry(
                InMemoryModuleEntry.ofFile(
                    "native.pb", NativeLibraries.getDefaultInstance().toByteArray()))
            .addEntry(
                InMemoryModuleEntry.ofFile(
                    "resources.pb", ResourceTable.getDefaultInstance().toByteArray()))
            .build();

    assertThat(bundleModule.getEntries()).isEmpty();
  }

  @Test
  public void baseAlwaysIncludedInFusing() throws Exception {
    BundleModule baseWithoutFusingConfig =
        createMinimalModuleBuilder()
            .setName(BundleModuleName.create("base"))
            .setAndroidManifestProto(androidManifest("com.test.app"))
            .build();
    assertThat(baseWithoutFusingConfig.isIncludedInFusing()).isTrue();

    BundleModule baseWithFusingConfigTrue =
        createMinimalModuleBuilder()
            .setName(BundleModuleName.create("base"))
            .setAndroidManifestProto(androidManifest("com.test.app", withFusingAttribute(true)))
            .build();
    assertThat(baseWithFusingConfigTrue.isIncludedInFusing()).isTrue();

    // This module is technically illegal and could not pass validations, but it makes sense for the
    // test.
    BundleModule baseWithFusingConfigFalse =
        createMinimalModuleBuilder()
            .setName(BundleModuleName.create("base"))
            .setAndroidManifestProto(androidManifest("com.test.app", withFusingAttribute(false)))
            .build();
    assertThat(baseWithFusingConfigFalse.isIncludedInFusing()).isTrue();
  }

  /** Tests that we skip directories that contain a directory that we want to find entries under. */
  @Test
  public void entriesUnderPath_withPrefixDirectory() throws Exception {
    ModuleEntry entry1 = InMemoryModuleEntry.ofFile("dir1/entry1", new byte[0]);
    ModuleEntry entry2 = InMemoryModuleEntry.ofFile("dir1/entry2", new byte[0]);
    ModuleEntry entry3 = InMemoryModuleEntry.ofFile("dir1longer/entry3", new byte[0]);

    BundleModule bundleModule =
        createMinimalModuleBuilder().addEntries(Arrays.asList(entry1, entry2, entry3)).build();

    assertThat(bundleModule.findEntriesUnderPath(ZipPath.create("dir1")).collect(toList()))
        .containsExactly(entry1, entry2);
  }

  @Test
  public void getEntry_existing_found() throws Exception {
    ModuleEntry entry = InMemoryModuleEntry.ofFile("dir/entry", new byte[0]);

    BundleModule bundleModule =
        createMinimalModuleBuilder().addEntries(Arrays.asList(entry)).build();

    assertThat(bundleModule.getEntry(ZipPath.create("dir/entry"))).hasValue(entry);
  }

  @Test
  public void getEntry_unknown_notFound() throws Exception {
    BundleModule bundleModule = createMinimalModuleBuilder().build();

    assertThat(bundleModule.getEntry(ZipPath.create("unknown-entry"))).isEmpty();
  }

  @Test
  public void getModuleMetadata_dependencies_parsedAndReturned() throws Exception {
    BundleModule bundleModule =
        createMinimalModuleBuilder()
            .setAndroidManifestProto(
                androidManifest("com.test.app", withUsesSplit("feature1", "feature2")))
            .build();

    assertThat(bundleModule.getModuleMetadata().getDependenciesList())
        .containsExactly("feature1", "feature2");
  }

  @Test
  public void getModuleMetadata_targeting_emptyIfNoConditions() throws Exception {
    BundleModule bundleModule =
        createMinimalModuleBuilder()
            .setAndroidManifestProto(androidManifest("com.test.app"))
            .build();
    assertThat(bundleModule.getModuleMetadata().getTargeting()).isEqualToDefaultInstance();
  }

  @Test
  public void getModuleMetadata_targeting_presentIfConditionsUsed() throws Exception {
    BundleModule bundleModule =
        createMinimalModuleBuilder()
            .setAndroidManifestProto(
                androidManifest(
                    "com.test.app",
                    withFeatureCondition("com.android.hardware.feature"),
                    withMinSdkCondition(24)))
            .build();
    assertThat(bundleModule.getModuleMetadata().getTargeting())
        .isEqualTo(
            mergeModuleTargeting(
                moduleFeatureTargeting("com.android.hardware.feature"),
                moduleMinSdkVersionTargeting(/* minSdkVersion= */ 24)));
  }

  private static BundleModule.Builder createMinimalModuleBuilder() {
    return BundleModule.builder()
        .setName(BundleModuleName.create("testModule"))
        .setAndroidManifestProto(androidManifest("com.test.app"))
        .setBundleConfig(DEFAULT_BUNDLE_CONFIG);
  }
}
