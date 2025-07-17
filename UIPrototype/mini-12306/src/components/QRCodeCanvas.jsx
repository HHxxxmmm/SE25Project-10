import React, { useEffect, useRef, useState } from 'react';
import QRCode from 'qrcode';

/**
 * 使用Canvas渲染二维码的组件
 * 作为QRCodeSVG的备用方案
 */
const QRCodeCanvas = ({ value, size = 200, level = 'M' }) => {
  const canvasRef = useRef(null);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (!value) {
      setError('没有提供有效数据');
      return;
    }

    const options = {
      errorCorrectionLevel: level,
      margin: 4,
      width: size,
      color: {
        dark: '#000',
        light: '#fff'
      }
    };

    try {
      QRCode.toCanvas(canvasRef.current, value, options, (err) => {
        if (err) {
          console.error('QRCodeCanvas渲染错误:', err);
          setError(err.message || '渲染二维码时出错');
        } else {
          setError(null);
          console.log('QRCodeCanvas渲染成功，数据长度:', value.length);
        }
      });
    } catch (e) {
      console.error('QRCodeCanvas异常:', e);
      setError(e.message || '生成二维码时发生异常');
    }
  }, [value, size, level]);

  return (
    <div className="qrcode-canvas-container">
      {error ? (
        <div className="qrcode-error" style={{ 
          color: 'red', 
          border: '1px dashed #ffaaaa',
          padding: '10px',
          marginBottom: '10px',
          backgroundColor: '#fff5f5'
        }}>
          渲染错误: {error}
        </div>
      ) : null}
      <canvas 
        ref={canvasRef} 
        style={{ 
          maxWidth: '100%',
          height: 'auto'
        }}
      />
    </div>
  );
};

export default QRCodeCanvas;
