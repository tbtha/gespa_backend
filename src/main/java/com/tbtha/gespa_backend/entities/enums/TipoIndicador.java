package com.tbtha.gespa_backend.entities.enums;

/**
 * Tipos de indicadores clínicos registrables en la evolución del paciente.
 * Se pueden agregar nuevos valores sin cambios estructurales en la entidad.
 */
public enum TipoIndicador {

    // Anthropometría
    PESO,
    TALLA,
    IMC,
    CIRCUNFERENCIA_ABDOMINAL,

    // Signos vitales
    PRESION_SISTOLICA,
    PRESION_DIASTOLICA,
    FRECUENCIA_CARDIACA,
    FRECUENCIA_RESPIRATORIA,
    TEMPERATURA,
    SATURACION_OXIGENO,

    // Laboratorio
    GLICEMIA,
    HEMOGLOBINA_GLICOSILADA,
    COLESTEROL_TOTAL,
    COLESTEROL_LDL,
    COLESTEROL_HDL,
    TRIGLICERIDOS,
    CREATININA,

    // Otro genérico (usar campo 'etiqueta' para especificar)
    OTRO
}
