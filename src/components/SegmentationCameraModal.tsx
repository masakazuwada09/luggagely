import React, { useMemo, useRef, useState } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Image,
  Dimensions,
  GestureResponderEvent,
} from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import AsyncStorage from '@react-native-async-storage/async-storage';
import {
  BaggageItem,
  BoundingBoxNorm,
  ContourPointNorm,
  FB_THEME,
} from '../types/baggage';
import {
  buildOrganicSamContour,
  runMuseSparkMetaSegmentation,
} from '../services/museSparkMetaClient';

export const ASYNC_STORAGE_ITEMS_KEY = '@luggagely_segmented_items_v1';

type WorkflowStep = 'CAPTURE_CAMERA' | 'SAM_IMAGE_EDITOR';
type SamPromptTool = 'ADD_POINT' | 'EXCLUDE_POINT' | 'DRAW_BOX';

interface SegmentationCameraModalProps {
  items: BaggageItem[];
  onClose: () => void;
  onItemsDetected: (detected: BaggageItem[]) => void;
}

/**
 * Full-Screen Camera -> SAM Interactive Image Editor -> Cutout Workflow:
 * 1. Capture Image First on the live camera screen
 * 2. Redirect to the SAM Image Editor to tap interactive points (+ / -) and boxes
 * 3. Show the "Cut Object" button to cut out the segmented silhouette and save to AsyncStorage & Dashboard
 */
export const SegmentationCameraModal: React.FC<SegmentationCameraModalProps> = ({
  items,
  onClose,
  onItemsDetected,
}) => {
  const [permission, requestPermission] = useCameraPermissions();
  const cameraRef = useRef<CameraView | null>(null);

  const [workflowStep, setWorkflowStep] = useState<WorkflowStep>('CAPTURE_CAMERA');
  const [capturedPhotoUri, setCapturedPhotoUri] = useState<string | null>(null);
  const [capturedBase64, setCapturedBase64] = useState<string | undefined>(undefined);

  const [activeTool, setActiveTool] = useState<SamPromptTool>('ADD_POINT');
  const [positivePoints, setPositivePoints] = useState<ContourPointNorm[]>([
    { x: 0.5, y: 0.48 },
  ]);
  const [negativePoints, setNegativePoints] = useState<ContourPointNorm[]>([]);
  const [interactiveBox, setInteractiveBox] = useState<BoundingBoxNorm | null>({
    xMin: 0.24,
    yMin: 0.24,
    width: 0.52,
    height: 0.48,
  });

  const [isCutting, setIsCutting] = useState(false);
  const [cutResultItem, setCutResultItem] = useState<BaggageItem | null>(null);

  // Step 1 -> Step 2: Capture image first, then redirect to SAM Image Editor
  const handleCaptureAndOpenEditor = async () => {
    let base64Frame: string | undefined;
    let uriFrame: string | undefined;
    try {
      if (cameraRef.current) {
        const photo = await cameraRef.current.takePictureAsync({
          base64: true,
          quality: 0.8,
          skipProcessing: false,
        });
        base64Frame = photo?.base64;
        uriFrame = photo?.uri;
      }
    } catch {
      // Fallback for simulator without hardware camera feed
    }

    setCapturedBase64(base64Frame);
    setCapturedPhotoUri(
      uriFrame || (base64Frame ? `data:image/jpeg;base64,${base64Frame}` : null)
    );
    setPositivePoints([{ x: 0.5, y: 0.48 }]);
    setNegativePoints([]);
    setInteractiveBox({
      xMin: 0.24,
      yMin: 0.24,
      width: 0.52,
      height: 0.48,
    });
    setCutResultItem(null);
    setWorkflowStep('SAM_IMAGE_EDITOR');
  };

  // Interactive tap handler inside SAM Image Editor for placing + Points, - Exclude Points, or Boxes
  const handleEditorCanvasPress = (evt: GestureResponderEvent) => {
    const { width, height } = Dimensions.get('window');
    const nx = Math.min(0.95, Math.max(0.05, evt.nativeEvent.locationX / Math.max(1, width)));
    const ny = Math.min(0.9, Math.max(0.1, evt.nativeEvent.locationY / Math.max(1, height)));
    setCutResultItem(null);

    if (activeTool === 'ADD_POINT') {
      setPositivePoints((prev) => [...prev, { x: nx, y: ny }]);
    } else if (activeTool === 'EXCLUDE_POINT') {
      setNegativePoints((prev) => [...prev, { x: nx, y: ny }]);
    } else {
      const halfW = 0.24;
      const halfH = 0.22;
      const xMin = Math.min(0.7, Math.max(0.06, nx - halfW));
      const yMin = Math.min(0.68, Math.max(0.12, ny - halfH));
      setInteractiveBox({
        xMin,
        yMin,
        width: Math.min(0.94 - xMin, halfW * 2),
        height: Math.min(0.88 - yMin, halfH * 2),
      });
    }
  };

  // Compute refined SAM bounding box & 36-point non-rectangular contour from interactive points and box
  const { activeBox, activeContour } = useMemo(() => {
    let box: BoundingBoxNorm = interactiveBox || {
      xMin: 0.24,
      yMin: 0.24,
      width: 0.52,
      height: 0.48,
    };

    if (positivePoints.length > 0 && !interactiveBox) {
      const xs = positivePoints.map((p) => p.x);
      const ys = positivePoints.map((p) => p.y);
      const minX = Math.max(0.06, Math.min(...xs) - 0.18);
      const maxX = Math.min(0.94, Math.max(...xs) + 0.18);
      const minY = Math.max(0.1, Math.min(...ys) - 0.18);
      const maxY = Math.min(0.88, Math.max(...ys) + 0.18);
      box = {
        xMin: minX,
        yMin: minY,
        width: Math.max(0.24, maxX - minX),
        height: Math.max(0.24, maxY - minY),
      };
    }

    const baseContour = buildOrganicSamContour(box);
    const cx = box.xMin + box.width * 0.5;
    const cy = box.yMin + box.height * 0.5;

    const refinedContour = baseContour.map((pt) => {
      let vx = pt.x;
      let vy = pt.y;
      for (const neg of negativePoints) {
        const dist = Math.hypot(vx - neg.x, vy - neg.y);
        if (dist < 0.16) {
          vx = cx + (vx - cx) * 0.72;
          vy = cy + (vy - cy) * 0.72;
        }
      }
      return { x: vx, y: vy };
    });

    return { activeBox: box, activeContour: refinedContour };
  }, [positivePoints, negativePoints, interactiveBox]);

  // Step 3: Execute SAM Cutout when the user taps the "Cut Object" button in the SAM Image Editor
  const handleCutObject = async () => {
    if (isCutting) return;
    setIsCutting(true);

    const result = await runMuseSparkMetaSegmentation(
      capturedBase64,
      capturedPhotoUri || undefined
    );
    const baseItem = result.detectedItems[0];
    if (baseItem) {
      const cutItem: BaggageItem = {
        ...baseItem,
        boundingBox: activeBox,
        contourPoints: activeContour,
      };
      setCutResultItem(cutItem);
      onItemsDetected([cutItem]);
      try {
        const updatedList = [
          cutItem,
          ...items.filter((existing) => existing.id !== cutItem.id),
        ];
        await AsyncStorage.setItem(ASYNC_STORAGE_ITEMS_KEY, JSON.stringify(updatedList));
      } catch {
        // Ignore AsyncStorage write error
      }
    }
    setIsCutting(false);
  };

  if (!permission) {
    return <View style={styles.fullScreenContainer} />;
  }

  if (!permission.granted) {
    return (
      <View style={styles.permissionContainer}>
        <View style={styles.permissionCard}>
          <Text style={styles.permissionTitle}>Camera Access Required</Text>
          <Text style={styles.permissionBody}>
            Allow camera access to capture a photo first, then open the SAM Image Editor to tap
            interactive points & boxes and cut out your object.
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

  const { width, height } = Dimensions.get('window');

  // =========================================================================
  // STEP 1: FULL-SCREEN CAMERA CAPTURE (Capture Image First)
  // =========================================================================
  if (workflowStep === 'CAPTURE_CAMERA') {
    return (
      <View style={styles.fullScreenContainer}>
        <CameraView ref={cameraRef} style={StyleSheet.absoluteFillObject} facing="back" />

        {/* Top Bar (Step 1: Capture Image) */}
        <View style={styles.floatingTopBar}>
          <TouchableOpacity style={styles.backButton} onPress={onClose}>
            <Text style={styles.backButtonText}>← Dashboard</Text>
          </TouchableOpacity>
          <View style={styles.headerCenter}>
            <Text style={styles.headerTitle}>Step 1 • Capture Image</Text>
            <Text style={styles.headerSubtitle} numberOfLines={1}>
              Capture photo to open SAM Image Editor
            </Text>
          </View>
          <View style={styles.savedCountBadge}>
            <Text style={styles.savedCountText}>{items.length} Saved</Text>
          </View>
        </View>

        {/* Bottom Capture Shutter Card — Redirects to SAM Image Editor */}
        <View style={styles.floatingBottomCard}>
          <View style={styles.captureActionRow}>
            <View style={{ flex: 1, marginRight: 12 }}>
              <Text style={styles.captureTitle}>Capture Image for SAM Editor</Text>
              <Text style={styles.captureSub}>
                Redirects to Image Editor to tap interactive points & boxes
              </Text>
            </View>
            <TouchableOpacity
              style={styles.captureRedirectBtn}
              onPress={handleCaptureAndOpenEditor}
            >
              <Text style={styles.captureRedirectBtnText}>Capture</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    );
  }

  // =========================================================================
  // STEP 2: SAM INTERACTIVE IMAGE EDITOR (Tap Points & Boxes -> Show Cut Button)
  // =========================================================================
  const hasPrompts = positivePoints.length > 0 || interactiveBox !== null;

  return (
    <View style={styles.fullScreenContainer}>
      {/* Interactive SAM Editor Canvas */}
      <TouchableOpacity
        activeOpacity={1}
        onPress={handleEditorCanvasPress}
        style={StyleSheet.absoluteFillObject}
      >
        {capturedPhotoUri ? (
          <Image
            source={{ uri: capturedPhotoUri }}
            style={StyleSheet.absoluteFillObject}
            resizeMode="cover"
          />
        ) : (
          <View style={[StyleSheet.absoluteFillObject, { backgroundColor: '#EAECEF' }]} />
        )}

        {/* Non-Square SAM Contour Polygon Overlay */}
        {hasPrompts &&
          activeContour.map((pt, idx) => {
            const nextPt = activeContour[(idx + 1) % activeContour.length];
            const x1 = pt.x * width;
            const y1 = pt.y * height;
            const x2 = nextPt.x * width;
            const y2 = nextPt.y * height;
            const dx = x2 - x1;
            const dy = y2 - y1;
            const length = Math.hypot(dx, dy);
            const angleRad = Math.atan2(dy, dx);
            return (
              <View
                key={`sam-contour-seg-${idx}`}
                style={[
                  styles.contourSegment,
                  {
                    width: length + 3,
                    left: (x1 + x2) / 2 - (length + 3) / 2,
                    top: (y1 + y2) / 2 - 2.5,
                    transform: [{ rotate: `${angleRad}rad` }],
                  },
                ]}
              />
            );
          })}

        {/* Interactive SAM Box Prompt Overlay */}
        {interactiveBox && (
          <View
            pointerEvents="none"
            style={[
              styles.samInteractiveBox,
              {
                left: interactiveBox.xMin * width,
                top: interactiveBox.yMin * height,
                width: interactiveBox.width * width,
                height: interactiveBox.height * height,
              },
            ]}
          />
        )}

        {/* Interactive Positive (+) SAM Points */}
        {positivePoints.map((pt, idx) => (
          <View
            key={`pos-pt-${idx}`}
            pointerEvents="none"
            style={[
              styles.samPointPin,
              styles.samPositivePin,
              {
                left: pt.x * width - 13,
                top: pt.y * height - 13,
              },
            ]}
          >
            <Text style={styles.samPointPinText}>+</Text>
          </View>
        ))}

        {/* Interactive Negative (-) Exclusion SAM Points */}
        {negativePoints.map((pt, idx) => (
          <View
            key={`neg-pt-${idx}`}
            pointerEvents="none"
            style={[
              styles.samPointPin,
              styles.samNegativePin,
              {
                left: pt.x * width - 13,
                top: pt.y * height - 13,
              },
            ]}
          >
            <Text style={styles.samPointPinText}>−</Text>
          </View>
        ))}
      </TouchableOpacity>

      {/* Top SAM Image Editor Header + Tool Selector Pills */}
      <View style={styles.editorTopColumn}>
        <View style={styles.floatingTopBar}>
          <TouchableOpacity style={styles.backButton} onPress={onClose}>
            <Text style={styles.backButtonText}>← Dashboard</Text>
          </TouchableOpacity>
          <View style={styles.headerCenter}>
            <Text style={styles.headerTitle}>SAM Image Editor</Text>
            <Text style={styles.headerSubtitle} numberOfLines={1}>
              Tap points (+) or box, then tap Cut Object
            </Text>
          </View>
          <TouchableOpacity
            style={styles.retakeButton}
            onPress={() => setWorkflowStep('CAPTURE_CAMERA')}
          >
            <Text style={styles.retakeButtonText}>Retake</Text>
          </TouchableOpacity>
        </View>

        <View style={styles.toolBarPillRow}>
          <TouchableOpacity
            style={[
              styles.toolChip,
              activeTool === 'ADD_POINT' && styles.toolChipActiveBlue,
            ]}
            onPress={() => setActiveTool('ADD_POINT')}
          >
            <Text
              style={[
                styles.toolChipText,
                activeTool === 'ADD_POINT' && styles.toolChipTextActive,
              ]}
            >
              + Point ({positivePoints.length})
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.toolChip,
              activeTool === 'EXCLUDE_POINT' && styles.toolChipActiveRed,
            ]}
            onPress={() => setActiveTool('EXCLUDE_POINT')}
          >
            <Text
              style={[
                styles.toolChipText,
                activeTool === 'EXCLUDE_POINT' && styles.toolChipTextActive,
              ]}
            >
              − Exclude ({negativePoints.length})
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={[
              styles.toolChip,
              activeTool === 'DRAW_BOX' && styles.toolChipActiveGreen,
            ]}
            onPress={() => setActiveTool('DRAW_BOX')}
          >
            <Text
              style={[
                styles.toolChipText,
                activeTool === 'DRAW_BOX' && styles.toolChipTextActive,
              ]}
            >
              {interactiveBox ? 'Box (Active)' : 'Draw Box'}
            </Text>
          </TouchableOpacity>

          <TouchableOpacity
            style={styles.resetChip}
            onPress={() => {
              setPositivePoints([]);
              setNegativePoints([]);
              setInteractiveBox(null);
              setCutResultItem(null);
            }}
          >
            <Text style={styles.resetChipText}>Reset</Text>
          </TouchableOpacity>
        </View>
      </View>

      {/* Bottom Card: Shows "Cut Object" Button & Cutout Result Preview */}
      <View style={styles.floatingBottomCard}>
        {cutResultItem ? (
          <View style={styles.cutoutPreviewRow}>
            {cutResultItem.imageUri ? (
              <Image
                source={{ uri: cutResultItem.imageUri }}
                style={styles.cutoutPreviewImage}
                resizeMode="contain"
              />
            ) : (
              <View style={styles.cutoutPlaceholder}>
                <Text style={styles.cutoutPlaceholderText}>
                  {cutResultItem.name.charAt(0).toUpperCase()}
                </Text>
              </View>
            )}
            <View style={styles.cutoutMetaCol}>
              <Text style={styles.cutoutBadge}>SAM CUTOUT SAVED</Text>
              <Text style={styles.cutoutItemName} numberOfLines={1}>
                {cutResultItem.name}
              </Text>
              <Text style={styles.cutoutItemSub}>
                {cutResultItem.weightKg.toFixed(2)} kg • Added to Dashboard list
              </Text>
            </View>
            <TouchableOpacity
              style={styles.newCaptureBtn}
              onPress={() => setCutResultItem(null)}
            >
              <Text style={styles.newCaptureBtnText}>Edit</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.donePillBtn} onPress={onClose}>
              <Text style={styles.donePillBtnText}>Done</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <View style={styles.captureActionRow}>
            <View style={{ flex: 1, marginRight: 12 }}>
              <Text style={styles.captureTitle}>
                {hasPrompts ? 'SAM Contour Ready to Cut' : 'Tap Object or Draw Box'}
              </Text>
              <Text style={styles.captureSub}>
                {positivePoints.length} include pt • {negativePoints.length} exclude pt
                {interactiveBox ? ' • 1 box' : ''}
              </Text>
            </View>

            <TouchableOpacity
              style={styles.cutObjectBtn}
              onPress={handleCutObject}
              disabled={isCutting}
            >
              {isCutting ? (
                <ActivityIndicator size="small" color="#FFFFFF" />
              ) : (
                <Text style={styles.cutObjectBtnText}>Cut Object</Text>
              )}
            </TouchableOpacity>
          </View>
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  fullScreenContainer: {
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
  editorTopColumn: {
    marginTop: 48,
    marginHorizontal: 14,
  },
  floatingTopBar: {
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderRadius: 28,
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  toolBarPillRow: {
    marginTop: 8,
    backgroundColor: 'rgba(255, 255, 255, 0.95)',
    borderRadius: 24,
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  toolChip: {
    flex: 1,
    paddingVertical: 7,
    paddingHorizontal: 8,
    borderRadius: 18,
    backgroundColor: FB_THEME.surfaceLight,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    marginHorizontal: 3,
    alignItems: 'center',
  },
  toolChipActiveBlue: {
    backgroundColor: FB_THEME.facebookBlue,
    borderColor: FB_THEME.facebookBlue,
  },
  toolChipActiveRed: {
    backgroundColor: FB_THEME.coralDanger,
    borderColor: FB_THEME.coralDanger,
  },
  toolChipActiveGreen: {
    backgroundColor: FB_THEME.emeraldSafe,
    borderColor: FB_THEME.emeraldSafe,
  },
  toolChipText: {
    fontSize: 11,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
  },
  toolChipTextActive: {
    color: '#FFFFFF',
  },
  resetChip: {
    paddingVertical: 7,
    paddingHorizontal: 10,
    borderRadius: 18,
    backgroundColor: FB_THEME.surfaceLight,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    marginLeft: 3,
  },
  resetChipText: {
    fontSize: 11,
    fontWeight: '700',
    color: FB_THEME.facebookBlue,
  },
  backButton: {
    backgroundColor: FB_THEME.surfaceLight,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    borderRadius: 18,
    paddingHorizontal: 12,
    paddingVertical: 7,
  },
  backButtonText: {
    color: FB_THEME.textPrimary,
    fontWeight: '700',
    fontSize: 12,
  },
  retakeButton: {
    backgroundColor: FB_THEME.facebookBlueSoft,
    borderWidth: 1,
    borderColor: FB_THEME.facebookBlue,
    borderRadius: 18,
    paddingHorizontal: 12,
    paddingVertical: 7,
  },
  retakeButtonText: {
    color: FB_THEME.facebookBlue,
    fontWeight: '800',
    fontSize: 12,
  },
  headerCenter: {
    alignItems: 'center',
    flex: 1,
    marginHorizontal: 8,
  },
  headerTitle: {
    fontSize: 14,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
  },
  headerSubtitle: {
    fontSize: 10,
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
  contourSegment: {
    position: 'absolute',
    height: 5,
    borderRadius: 3,
    backgroundColor: FB_THEME.facebookBlue,
    borderWidth: 1,
    borderColor: '#FFFFFF',
  },
  samInteractiveBox: {
    position: 'absolute',
    borderWidth: 2.5,
    borderColor: FB_THEME.emeraldSafe,
    borderRadius: 12,
    backgroundColor: 'rgba(16, 185, 129, 0.08)',
  },
  samPointPin: {
    position: 'absolute',
    width: 26,
    height: 26,
    borderRadius: 13,
    borderWidth: 2.5,
    borderColor: '#FFFFFF',
    alignItems: 'center',
    justifyContent: 'center',
  },
  samPositivePin: {
    backgroundColor: FB_THEME.facebookBlue,
  },
  samNegativePin: {
    backgroundColor: FB_THEME.coralDanger,
  },
  samPointPinText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '900',
    lineHeight: 17,
  },
  floatingBottomCard: {
    marginHorizontal: 14,
    marginBottom: 24,
    backgroundColor: 'rgba(255, 255, 255, 0.97)',
    padding: 14,
    borderRadius: 22,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
  },
  cutoutPreviewRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  cutoutPreviewImage: {
    width: 56,
    height: 56,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    backgroundColor: FB_THEME.facebookBlueSoft,
  },
  cutoutPlaceholder: {
    width: 56,
    height: 56,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    backgroundColor: FB_THEME.facebookBlueSoft,
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
    marginHorizontal: 10,
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
  newCaptureBtn: {
    backgroundColor: FB_THEME.surfaceLight,
    borderWidth: 1,
    borderColor: FB_THEME.borderLight,
    borderRadius: 18,
    paddingHorizontal: 12,
    paddingVertical: 9,
    marginRight: 6,
  },
  newCaptureBtnText: {
    color: FB_THEME.facebookBlue,
    fontWeight: '700',
    fontSize: 12,
  },
  donePillBtn: {
    backgroundColor: FB_THEME.facebookBlue,
    borderRadius: 18,
    paddingHorizontal: 14,
    paddingVertical: 9,
  },
  donePillBtnText: {
    color: '#FFFFFF',
    fontWeight: '700',
    fontSize: 13,
  },
  captureActionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  captureTitle: {
    fontSize: 15,
    fontWeight: '800',
    color: FB_THEME.textPrimary,
  },
  captureSub: {
    fontSize: 11,
    color: FB_THEME.textSecondary,
    marginTop: 2,
  },
  captureRedirectBtn: {
    backgroundColor: FB_THEME.facebookBlue,
    borderRadius: 22,
    paddingHorizontal: 20,
    paddingVertical: 12,
  },
  captureRedirectBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 14,
  },
  cutObjectBtn: {
    backgroundColor: FB_THEME.facebookBlue,
    borderRadius: 22,
    paddingHorizontal: 20,
    paddingVertical: 12,
    minWidth: 116,
    alignItems: 'center',
  },
  cutObjectBtnText: {
    color: '#FFFFFF',
    fontWeight: '800',
    fontSize: 14,
  },
});
