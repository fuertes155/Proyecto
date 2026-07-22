import 'dart:async';
import 'dart:math';

import 'package:flutter/gestures.dart';
import 'package:sensors_plus/sensors_plus.dart';

/// Simula un SDK Comercial de Biometría Conductual (como BioCatch o SecuredTouch).
/// Analiza la telemetría del usuario (velocidad de tipeo, presión táctil, ángulo del dispositivo)
/// para detectar bots o posibles ladrones usando el teléfono de forma errática.
class BehavioralBiometricsService {
  static final BehavioralBiometricsService _instance = BehavioralBiometricsService._internal();

  factory BehavioralBiometricsService() => _instance;

  BehavioralBiometricsService._internal();

  // Telemetría
  final List<double> _pressures = [];
  final List<int> _keystrokeTimestamps = [];
  
  // Acelerómetro (Vibraciones / Temblores)
  StreamSubscription<AccelerometerEvent>? _accelSubscription;
  double _totalAccelerationDelta = 0.0;
  AccelerometerEvent? _lastEvent;
  int _accelEventCount = 0;

  void startSession() {
    _pressures.clear();
    _keystrokeTimestamps.clear();
    _totalAccelerationDelta = 0.0;
    _lastEvent = null;
    _accelEventCount = 0;

    _accelSubscription = accelerometerEventStream().listen((AccelerometerEvent event) {
      if (_lastEvent != null) {
        // Calculamos el cambio brusco (Delta) de movimiento
        double deltaX = (event.x - _lastEvent!.x).abs();
        double deltaY = (event.y - _lastEvent!.y).abs();
        double deltaZ = (event.z - _lastEvent!.z).abs();
        
        _totalAccelerationDelta += (deltaX + deltaY + deltaZ);
        _accelEventCount++;
      }
      _lastEvent = event;
    });
  }

  void stopSession() {
    _accelSubscription?.cancel();
  }

  void recordPointerEvent(PointerEvent event) {
    if (event is PointerDownEvent || event is PointerMoveEvent) {
      // Registrar presión si el dispositivo lo soporta
      if (event.pressure > 0.0 && event.pressure != 1.0) {
        _pressures.add(event.pressure);
      }
    }
  }

  void recordKeystroke() {
    _keystrokeTimestamps.add(DateTime.now().millisecondsSinceEpoch);
  }

  /// Retorna TRUE si se detecta un comportamiento anómalo / fraude.
  bool analyzeAndDetectFraud() {
    stopSession();

    // 1. Análisis de Velocidad de Tipeo (Bot Detection)
    if (_keystrokeTimestamps.length >= 3) {
      int totalTime = 0;
      for (int i = 1; i < _keystrokeTimestamps.length; i++) {
        totalTime += (_keystrokeTimestamps[i] - _keystrokeTimestamps[i - 1]);
      }
      double avgKeystrokeTimeMs = totalTime / (_keystrokeTimestamps.length - 1);
      
      // Si el promedio entre teclas es menor a 50ms, es casi seguro un script automatizado (bot)
      if (avgKeystrokeTimeMs < 50) {
        return true; 
      }
    }

    // 2. Análisis de Presión Táctil
    if (_pressures.isNotEmpty) {
      double avgPressure = _pressures.reduce((a, b) => a + b) / _pressures.length;
      // Umbral simulado de presión excesiva (varía por hardware, pero sirve para la prueba heurística)
      if (avgPressure > 3.0) {
        return true;
      }
    }

    // 3. Análisis de Movimiento (Agitación violenta o "Snatch-and-Run")
    if (_accelEventCount > 10) {
      double avgDelta = _totalAccelerationDelta / _accelEventCount;
      // Si el teléfono se está moviendo bruscamente en muchos ejes (ej. el ladrón corriendo)
      if (avgDelta > 20.0) {
        return true;
      }
    }

    return false; // Todo normal
  }
}
