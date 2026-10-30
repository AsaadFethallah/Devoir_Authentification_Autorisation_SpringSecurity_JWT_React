package ma.formation.incidentsapi.dto;

public record StatisticsResponse(
        long totalUsers,
        long employees,
        long technicians,
        long totalIncidents,
        long open,
        long inProgress,
        long closed
) {
}
