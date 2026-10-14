package pe.viadeep.incident.domain;

/** Coordenadas GPS en WGS 84. */
public record GeoLocation(double latitude, double longitude) {

    public GeoLocation {
        if (latitude < -90 || latitude > 90 || Double.isNaN(latitude)) {
            throw new InvalidIncidentException("La latitud debe estar entre -90 y 90");
        }
        if (longitude < -180 || longitude > 180 || Double.isNaN(longitude)) {
            throw new InvalidIncidentException("La longitud debe estar entre -180 y 180");
        }
    }
}
