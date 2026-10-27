import React, { useRef, useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Image,
} from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { BaggageItem, FB_THEME } from '../types/baggage';
import { runMuseSparkMetaSegmentation } from '../services/museSparkMetaClient';

export const ASYNC_STORAGE_ITEMS_KEY = '@luggagely_segmented_items_v1';

interface SegmentationCameraModalProps {
  items: BaggageItem[];
  onClose: () => void;
  onItemsDetected: (detected: BaggageItem[]) => void;
}

export const SegmentationCameraModal: React.FC<SegmentationCameraModalProps> = ({
  items,
  onClose,
  onItemsDetected,
}) => {
  const [permission, requestPermission] = useCameraPermissions();
  const cameraRef = useRef<CameraView | null>(null);
  const [isScanning, setIsScanning] = useState(false);
  const [lastSegmentedItem, setLastSegmentedItem] = useState<BaggageItem | null>(
    items.find((i) => i.source === 'AI_SCANNED') || null
  );
  const [statusMessage, setStatusMessage] = useState(
    'Center a single item in the frame to segment and cut out its image'
  );

  const handleSegmentSingleItem = async () => {
    if (isScanning) return;
    setIsScanning(true);
    setStatusMessage('Segmenting single item & creating cutout image...');

    let base64Frame: string | undefined;
    let capturedUri: string | undefined;
    try {
      if (cameraRef.current) {
        const photo = await cameraRef.current.takePictureAsync({
          base64: true,
          quality: 0.72,
        });
        base64Frame = photo?.base64;
        capturedUri = photo?.uri;
      }
    } catch {
      // Proceed even if simulator camera capture is unavailable
    }

    const result = await runMuseSparkMetaSegmentation(base64Frame, capturedUri);
    const singleItem = result.detectedItems[0];
    if (singleItem) {
      setLastSegmentedItem(singleItem);
      onItemsDetected([singleItem]);
      try {
        const updatedList = [
          singleItem,
          ...items.filter((existing) => existing.id !== singleItem.id),
        ];
        await AsyncStorage.setItem(ASYNC_STORAGE_ITEMS_KEY, JSON.stringify(updatedList));
      } catch {
        // Ignore AsyncStorage write error
      }
      setStatusMessage(
        `Segmented "${singleItem.name}" (${singleItem.weightKg.toFixed(2)} kg) • Saved to AsyncStorage`
      );
    }
    setIsScanning(false);
  };

  if (!permission) {
    return <View style={styles.container} />;
  }

  if (!permission.granted) {
    return (
      <View style={styles.permissionContainer}>
        <View style={styles.permissionCard}>
          <Text style={styles.permissionTitle}>Camera Access Required</Text>
          <Text style={styles.permissionBody}>
            Allow camera access to segment a single item with SAM, cut out its image, and save it to
            your Dashboard item list.
          </Text>
          <TouchableOpacity style={styles.primaryBtn} onPress={requestPermission}>
            <Text style={styles.primaryBtnText}>Allow Camera</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.secondaryBtn} onPress={onClose}>
            <Text style={styles.secondaryBtnText}>Back to Dashboard</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  const activeBox = lastSegmentedItem?.boundingBox;

  return (
    <View style={styles.container}>
      {/* Dashboard-Themed Top Bar (No MUSE-SPARK-1.3 // META SAM READY badge, no rig preset tabs) */}
      <View style={styles.topHeaderCard}>
        <TouchableOpacity style={styles.backButton} onPress={onClose}>
          <Text style={styles.backButtonText}>← Dashboard</Text>
        </TouchableOpacity>
        <View style={styles.headerCenter}>
          <Text style={styles.headerTitle}>SAM Item Scanner</Text>
          <Text style={styles.headerSubtitle}>Single-Item Cutout & Label</Text>
        </View>
        <View style={styles.savedCountBadge}>
          <Text style={styles.savedCountText}>{items.length} Saved</Text>
        </View>
      </View>

      {/* Camera Viewport Card styled like Dashboard Cards */}
      <View style={styles.viewportCard}>
        <CameraView ref={cameraRef} style={StyleSheet.absoluteFillObject} facing="back" />

        {/* Single-Item SAM Solid Highlight & Label (No scanning animation, no dashed shapes) */}
        {lastSegmentedItem && activeBox ? (
          <View
            pointerEvents="none"
            style={[
              styles.singleHighlightBox,
              {
                left: `${Math.round(activeBox.xMin * 100)}%`,
                top: `${Math.round(activeBox.yMin * 100)}%`,
                width: `${Math.round(activeBox.width * 100)}%`,
                height: `${Math.round(activeBox.height * 100)}%`,
              },
            ]}
          >
            <View style={styles.highlightLabelPill}>
              <View style={styles.highlightDot} />
              <Text style={styles.highlightLabelText} numberOfLines={1}>
                {lastSegmentedItem.name} • {lastSegmentedItem.weightKg.toFixed(2)} kg
              </Text>
            </View>
          </View>
        ) : (
          <View pointerEvents="none" style={styles.focusGuideFrame} />
        )}
      </View>

      {/* Bottom Dashboard-Themed Controls & Cutout Preview Card */}
      <View style={styles.bottomCard}>
        {lastSegmentedItem && (
          <View style={styles.cutoutPreviewRow}>
            {lastSegmentedItem.imageUri ? (
              <Image
                source={{ uri: lastSegmentedItem.imageUri }}
                style={styles.cutoutPreviewImage}
              />
            ) : (
              <View style={styles.cutoutPlaceholder}>
                <Text style={styles.cutoutPlaceholderText}>
                  {lastSegmentedItem.name.charAt(0).toUpperCase()}
                </Text>
              </View>
            )}
            <View style={styles.cutoutMetaCol}>
              <Text style={styles.cutoutBadge}>SAM CUTOUT SAVED TO ASYNCSTORAGE</Text>
              <Text style={styles.cutoutItemName} numberOfLines={1}>
                {lastSegmentedItem.name}
              </Text>
              <Text style={styles.cutoutItemSub}>
                {lastSegmentedItem.weightKg.toFixed(2)} kg • Displayed on Dashboard Item List
              </Text>
            </View>
          </View>
        )}

        <Text style={styles.statusText}>{statusMessage}</Text>

        <TouchableOpacity
          style={[styles.scanButton, isScanning && { opacity: 0.75 }]}
          onPress={handleSegmentSingleItem}
          disabled={isScanning}
        >
          {isScanning ? (
            <ActivityIndicator color="#FFFFFF" />
          ) : (
            <Text style={styles.scanButtonText}>Segment & Cut Out Item</Text>
          )}
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: FB_THEME.feedGrayBg,
    padding: 14,
    justifyContent: 'space-between',
  },
  permissionContainer: {
    flex: 1,
    backgroundColor: FB_THEME.feedGrayBg,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  permissionCard: {
    width: '100%',
    backgroundColor: FB_THEME.cardWhite,
    borderRadius: 16,
    padding: 24,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  permissionTitle: {
    fontSize: 20,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
    marginBottom: 8,
  },
  permissionBody: {
    fontSize: 14,
    color: FB_THEME.textSecondary,
    lineHeight: 20,
    marginBottom: 20,
  },
  primaryBtn: {
    backgroundColor: FB_THEME.facebookBlue,
    borderRadius: 10,
    paddingVertical: 14,
    alignItems: 'center',
    marginBottom: 10,
  },
  primaryBtnText: {
    color: FB_THEME.cardWhite,
    fontWeight: '700',
    fontSize: 15,
  },
  secondaryBtn: {
    backgroundColor: FB_THEME.chipGray,
    borderRadius: 10,
    paddingVertical: 12,
    alignItems: 'center',
  },
  secondaryBtnText: {
    color: FB_THEME.textPrimary,
    fontWeight: '600',
    fontSize: 14,
  },
  topHeaderCard: {
    backgroundColor: FB_THEME.cardWhite,
    borderRadius: 16,
    paddingHorizontal: 14,
    paddingVertical: 12,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  backButton: {
    backgroundColor: FB_THEME.surfaceLight,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 8,
  },
  backButtonText: {
    color: FB_THEME.textPrimary,
    fontWeight: '700',
    fontSize: 13,
  },
  headerCenter: {
    alignItems: 'center',
    flex: 1,
    marginHorizontal: 8,
  },
  headerTitle: {
    fontSize: 16,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
  },
  headerSubtitle: {
    fontSize: 11,
    color: FB_THEME.textSecondary,
    marginTop: 1,
  },
  savedCountBadge: {
    backgroundColor: FB_THEME.facebookBlueSoft,
    borderWidth: 1,
    borderColor: FB_THEME.facebookBlue,
    borderRadius: 16,
    paddingHorizontal: 10,
    paddingVertical: 5,
  },
  savedCountText: {
    fontSize: 11,
    fontWeight: '800',
    color: FB_THEME.facebookBlue,
  },
  viewportCard: {
    flex: 1,
    marginVertical: 12,
    borderRadius: 18,
    overflow: 'hidden',
    backgroundColor: FB_THEME.cardWhite,
    borderWidth: 1.5,
    borderColor: FB_THEME.borderLight,
    justifyContent: 'center',
    alignItems: 'center',
  },
  focusGuideFrame: {
    width: '62%',
    height: '54%',
    borderRadius: 20,
    borderWidth: 2,
    borderColor: 'rgba(255, 255, 255, 0.75)',
  },
  singleHighlightBox: {
    position: 'absolute',
    borderWidth: 3,
    borderColor: FB_THEME.facebookBlue,
    backgroundColor: 'rgba(24, 119, 242, 0.20)',
    borderRadius: 18,
  },
  highlightLabelPill: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: FB_THEME.cardWhite,
    borderWidth: 1.5,
    borderColor: FB_THEME.facebookBlue,
    alignSelf: 'flex-start',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 20,
    margin: 8,
  },
  highlightDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: FB_THEME.facebookBlue,
    marginRight: 6,
  },
  highlightLabelText: {
    color: FB_THEME.textPrimary,
    fontSize: 12,
    fontWeight: '800',
  },
  bottomCard: {
    backgroundColor: FB_THEME.cardWhite,
    padding: 16,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  cutoutPreviewRow: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: FB_THEME.facebookBlueSoft,
    borderWidth: 1,
    borderColor: FB_THEME.facebookBlue,
    borderRadius: 12,
    padding: 10,
    marginBottom: 12,
  },
  cutoutPreviewImage: {
    width: 54,
    height: 54,
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: FB_THEME.facebookBlue,
    backgroundColor: FB_THEME.cardWhite,
  },
  cutoutPlaceholder: {
    width: 54,
    height: 54,
    borderRadius: 12,
    borderWidth: 1.5,
    borderColor: FB_THEME.facebookBlue,
    backgroundColor: FB_THEME.cardWhite,
    alignItems: 'center',
    justifyContent: 'center',
  },
  cutoutPlaceholderText: {
    fontSize: 18,
    fontWeight: '800',
    color: FB_THEME.facebookBlue,
  },
  cutoutMetaCol: {
    flex: 1,
    marginLeft: 12,
  },
  cutoutBadge: {
    fontSize: 9,
    fontWeight: '800',
    color: FB_THEME.facebookBlue,
  },
  cutoutItemName: {
    fontSize: 15,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
    marginTop: 2,
  },
  cutoutItemSub: {
    fontSize: 11,
    color: FB_THEME.textSecondary,
    marginTop: 2,
  },
  statusText: {
    fontSize: 12,
    fontWeight: '600',
    color: FB_THEME.textSecondary,
    marginBottom: 10,
    textAlign: 'center',
  },
  scanButton: {
    backgroundColor: FB_THEME.facebookBlue,
    borderRadius: 12,
    paddingVertical: 14,
    alignItems: 'center',
  },
  scanButtonText: {
    color: FB_THEME.cardWhite,
    fontSize: 15,
    fontWeight: '700',
  },
});
