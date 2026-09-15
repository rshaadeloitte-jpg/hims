import { useState, useRef, useCallback } from "react";

/**
 * PhotoCapture — camera capture + file upload for patient photos.
 * Uses getUserMedia API with fallback to file input.
 */
export function PhotoCapture({
  value,
  onChange,
  disabled = false,
}: {
  value?: string; // base64 data URL
  onChange: (dataUrl: string, file: File) => void;
  disabled?: boolean;
}) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [stream, setStream] = useState<MediaStream | null>(null);
  const [error, setError] = useState<string | undefined>();
  const [isCapturing, setIsCapturing] = useState(false);

  const startCamera = useCallback(async () => {
    try {
      setError(undefined);
      const mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: "user", width: { ideal: 640 }, height: { ideal: 480 } },
      });
      setStream(mediaStream);
      setIsCapturing(true);
      if (videoRef.current) {
        videoRef.current.srcObject = mediaStream;
      }
    } catch {
      setError("Camera unavailable. Use file upload instead.");
      setIsCapturing(false);
    }
  }, []);

  const stopCamera = useCallback(() => {
    if (stream) {
      stream.getTracks().forEach((t) => t.stop());
      setStream(null);
    }
    setIsCapturing(false);
  }, [stream]);

  const capture = useCallback(() => {
    if (!videoRef.current || !canvasRef.current) return;
    const video = videoRef.current;
    const canvas = canvasRef.current;
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.drawImage(video, 0, 0);
    const dataUrl = canvas.toDataURL("image/jpeg", 0.85);
    // Convert to File
    const blob = dataURLtoBlob(dataUrl);
    const file = new File([blob], `patient-photo-${Date.now()}.jpg`, { type: "image/jpeg" });
    onChange(dataUrl, file);
    stopCamera();
  }, [onChange, stopCamera]);

  const handleFileSelect = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (!file) return;
      if (!file.type.startsWith("image/")) {
        setError("Select an image file");
        return;
      }
      if (file.size > 5 * 1024 * 1024) {
        setError("Image must be under 5 MB");
        return;
      }
      setError(undefined);
      const reader = new FileReader();
      reader.onload = () => {
        const dataUrl = reader.result as string;
        onChange(dataUrl, file);
      };
      reader.readAsDataURL(file);
    },
    [onChange],
  );

  const clear = useCallback(() => {
    onChange("", new File([], ""));
    stopCamera();
  }, [onChange, stopCamera]);

  return (
    <div className="photo-capture">
      <label className="field">
        <span className="field__label">Patient Photo</span>
        <div className="photo-capture__body">
          {value ? (
            <div className="photo-capture__preview">
              <img src={value} alt="Patient" className="photo-capture__img" />
              <button
                type="button"
                className="btn btn-secondary btn-sm photo-capture__clear"
                onClick={clear}
                disabled={disabled}
              >
                Remove
              </button>
            </div>
          ) : isCapturing ? (
            <div className="photo-capture__camera">
              <video ref={videoRef} autoPlay playsInline muted className="photo-capture__video" />
              <canvas ref={canvasRef} className="photo-capture__canvas" aria-hidden="true" />
              <div className="photo-capture__camera-actions">
                <button
                  type="button"
                  className="btn btn-primary"
                  onClick={capture}
                  disabled={disabled}
                >
                  Capture
                </button>
                <button
                  type="button"
                  className="btn btn-secondary"
                  onClick={stopCamera}
                >
                  Cancel
                </button>
              </div>
            </div>
          ) : (
            <div className="photo-capture__placeholder">
              <span className="photo-capture__icon" aria-hidden="true">📷</span>
              <div className="photo-capture__actions">
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={startCamera}
                  disabled={disabled}
                >
                  Open Camera
                </button>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={disabled}
                >
                  Upload File
                </button>
              </div>
            </div>
          )}
        </div>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          className="visually-hidden"
          onChange={handleFileSelect}
          disabled={disabled}
          aria-label="Upload patient photo"
        />
        {error && (
          <span className="field-error" role="alert">{error}</span>
        )}
      </label>
    </div>
  );
}

function dataURLtoBlob(dataUrl: string): Blob {
  const [header, base64] = dataUrl.split(",");
  const mime = header.match(/:(.*?);/)?.[1] ?? "image/jpeg";
  const binary = atob(base64);
  const array = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) {
    array[i] = binary.charCodeAt(i);
  }
  return new Blob([array], { type: mime });
}
