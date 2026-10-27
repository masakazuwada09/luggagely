import React, { useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Dimensions,
} from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { BaggageItem, FB_THEME } from '../types/baggage';
import {
  MUSE_SPARK_MODEL,
  META_SAM_SEGMENTATION_MODEL,
  runMuseSparkMetaSegmentation,
} from '../services/museSparkMetaClient';

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
  const [isScanning, setIsScanning] = useState(false);
  const [statusMessage, setStatusMessage] = useState(
    `Ready • ${MUSE_SPARK_MODEL} + Meta SAM 2.1 Active`
  );

  const handleScanNow = async () => {
    if (isScanning) return;
    setIsScanning(true);
    setStatusMessage(`Segmenting masks via ${MUSE_SPARK_MODEL}...`);
    const result = await runMuseSparkMetaSegmentation();
    onItemsDetected(result.detectedItems);
    setIsScanning(false);
    setStatusMessage(
      `Detected ${result.detectedItems.length} items (${result.latencyMs}ms • ${result.modelUsed})`
    );
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
            Allow native iOS / Android camera access to run real-time Meta SAM segmentation and{' '}
            {MUSE_SPARK_MODEL} baggage weight estimation.
          </Text>
          <TouchableOpacity style={styles.primaryBtn} onPress={requestPermission}>
            <Text style={styles.primaryBtnText}>Allow Camera & Start Segmentation</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.secondaryBtn} onPress={onClose}>
            <Text style={styles.secondaryBtnText}>Return to Dashboard</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  const { width, height } = Dimensions.get('window');
  const segmentedItems = items.filter((i) => i.boundingBox);

  return (
    <View style={styles.container}>
      <CameraView style={StyleSheet.absoluteFillObject} facing="back" />

      {/* Meta SAM Bounding Box Overlays */}
      <View style={StyleSheet.absoluteFillObject} pointerEvents="none">
        {segmentedItems.map((item) => {
          const box = item.boundingBox!;
          return (
            <View
              key={item.id}
              style={[
                styles.boundingBox,
                {
                  left: box.xMin * width,
                  top: box.yMin * height * 0.7,
                  width: box.width * width,
                  height: box.height * height * 0.55,
                },
              ]}
            >
              <View style={styles.boxTag}>
                <Text style={styles.boxTagText}>
                  {item.name} • {item.weightKg.toFixed(2)} kg
                </Text>
              </View>
            </View>
          );
        })}
      </View>

      {/* Top HUD */}
      <View style={styles.topHud}>
        <View style={styles.hudBadge}>
          <View style={styles.liveDot} />
          <Text style={styles.hudBadgeText}>
            {MUSE_SPARK_MODEL.toUpperCase()} • {META_SAM_SEGMENTATION_MODEL}
          </Text>
        </View>
        <TouchableOpacity style={styles.closeBtn} onPress={onClose}>
          <Text style={styles.closeBtnText}>✕</Text>
        </TouchableOpacity>
      </View>

      {/* Bottom Controls */}
      <View style={styles.bottomSheet}>
        <Text style={styles.statusText}>{statusMessage}</Text>
        <TouchableOpacity
          style={[styles.scanButton, isScanning && { opacity: 0.75 }]}
          onPress={handleScanNow}
          disabled={isScanning}
        >
          {isScanning ? (
            <ActivityIndicator color="#FFFFFF" />
          ) : (
            <Text style={styles.scanButtonText}>Scan & Segment Gear ({MUSE_SPARK_MODEL})</Text>
          )}
        </TouchableOpacity>
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000000',
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
  topHud: {
    marginTop: 54,
    paddingHorizontal: 16,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  hudBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: 'rgba(255,255,255,0.92)',
    paddingHorizontal: 12,
    paddingVertical: 8,
    borderRadius: 20,
  },
  liveDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: FB_THEME.emeraldSafe,
    marginRight: 8,
  },
  hudBadgeText: {
    fontSize: 11,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
  },
  closeBtn: {
    width: 38,
    height: 38,
    borderRadius: 19,
    backgroundColor: 'rgba(255,255,255,0.92)',
    alignItems: 'center',
    justifyContent: 'center',
  },
  closeBtnText: {
    fontSize: 16,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
  },
  boundingBox: {
    position: 'absolute',
    borderWidth: 2,
    borderColor: FB_THEME.facebookBlue,
    backgroundColor: 'rgba(24, 119, 242, 0.14)',
    borderRadius: 10,
  },
  boxTag: {
    backgroundColor: FB_THEME.facebookBlue,
    alignSelf: 'flex-start',
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderTopLeftRadius: 8,
    borderBottomRightRadius: 8,
  },
  boxTagText: {
    color: '#FFFFFF',
    fontSize: 11,
    fontWeight: '700',
  },
  bottomSheet: {
    backgroundColor: FB_THEME.cardWhite,
    padding: 20,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
  },
  statusText: {
    fontSize: 13,
    fontWeight: '600',
    color: FB_THEME.textSecondary,
    marginBottom: 12,
    textAlign: 'center',
  },
  scanButton: {
    backgroundColor: FB_THEME.facebookBlue,
    borderRadius: 12,
    paddingVertical: 15,
    alignItems: 'center',
  },
  scanButtonText: {
    color: FB_THEME.cardWhite,
    fontSize: 15,
    fontWeight: '700',
  },
});
