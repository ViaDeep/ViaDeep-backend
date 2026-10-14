package pe.viadeep.incident.domain;

/** Catálogo de tipos de incidencia: las cuatro clases que detecta LMD-YOLO (RDD2022). */
public enum IncidentType {
    D00("Grieta longitudinal"),
    D10("Grieta transversal"),
    D20("Piel de cocodrilo"),
    D40("Bache");

    private final String displayName;

    IncidentType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
