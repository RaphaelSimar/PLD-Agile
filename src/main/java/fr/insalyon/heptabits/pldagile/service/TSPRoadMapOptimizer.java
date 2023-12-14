package fr.insalyon.heptabits.pldagile.service;

import fr.insalyon.heptabits.pldagile.model.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.Double.MAX_VALUE;

public class TSPRoadMapOptimizer implements RoadMapOptimizer {
    private final RoadMapBuilder roadMapBuilder;

    /**
     * Default constructor
     * Courier speed is 15 km/h
     * Delivery duration is 5 minutes
     */
    public TSPRoadMapOptimizer(RoadMapBuilder roadMapBuilder) {
        this.roadMapBuilder = roadMapBuilder;
    }


    private boolean aRequestIsBeforeDeparture(Collection<DeliveryRequest> requests, LocalTime departureTime) {
        for (DeliveryRequest request : requests) {
            if (request.getTimeWindow().getEnd().isBefore(departureTime)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public RoadMap optimize(Collection<DeliveryRequest> requests, Map map, LocalTime departureTime) throws ImpossibleRoadMapException {
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("No requests");
        }

        if (aRequestIsBeforeDeparture(requests, departureTime)) {
            throw new IllegalArgumentException("One request's time window ends before the courier's departure. The user shouldn't have been able to create such a request.");
        }

        // Check if a request is made at warehouse
        for (DeliveryRequest request : requests) {
            if (request.getDestination().equals(map.getWarehouse())) {
                throw new IllegalArgumentException("A request is made at the warehouse");
            }
        }

        // Assert that all requests are for the same day
        if (requests.stream().map(DeliveryRequest::getDate).distinct().count() != 1) {
            // This could be an issue in the future if we want to make night deliveries. But for now, we don't.
            throw new IllegalArgumentException("All requests must be for the same day");
        }

        // Sort requests by timeWindow start
        List<DeliveryRequest> sortedRequests = new ArrayList<>(requests);
        sortedRequests.sort((r1, r2) -> r1.getTimeWindow().compareStartTo(r2.getTimeWindow()));

        List<DeliveryRequest> sortedOptimizedRequests = new ArrayList<>();

        List<DeliveryRequest> requestsByTimeWindow = new ArrayList<>();
        TimeWindow timeWindow = sortedRequests.get(0).getTimeWindow();
        Intersection start = map.getWarehouse();
        for(DeliveryRequest request : sortedRequests){
            if(timeWindow.getStart() != request.getTimeWindow().getStart()) {
                sortedOptimizedRequests.addAll(getOptimizeItinerary(requestsByTimeWindow, map, start));
                requestsByTimeWindow.clear();
                start = sortedOptimizedRequests.getLast().getDestination();
                timeWindow = request.getTimeWindow();
            }
            requestsByTimeWindow.add(request);
        }
        sortedOptimizedRequests.addAll(getOptimizeItinerary(requestsByTimeWindow, map, start));

        return roadMapBuilder.buildRoadMapFromSortedRequests(sortedOptimizedRequests, map);
    }

    public List<DeliveryRequest> getOptimizeItinerary(List<DeliveryRequest> requests, Map map, Intersection start) {

        //Obtenir la liste de chemins possibles
        List<List<DeliveryRequest>> possiblePaths = generatePaths(requests);

        //Calcul des chemins pour avoir le plus optimal
        List<DeliveryRequest> sortedRequests = new ArrayList<>();
        double minimumCost = MAX_VALUE;
        for(List<DeliveryRequest> possiblePath : possiblePaths){
            //Initialisation pour chaque itinéraire
            double currentCost = 0;

            //Calcul de la distance entre le départ et le premier élément de la liste
            List<Intersection> firstStepIntersections = map.getShortestPath(start, possiblePath.get(0).getDestination());
            List<Segment> firstSegments = map.getShortestSegmentsBetween(firstStepIntersections);
            for(int k=0; k<firstSegments.size(); k++){
                currentCost+=firstSegments.get(k).length();
            }

            //Calcul de la distance entre les différents éléments de la liste
            int j = 0;
            while(minimumCost>currentCost && j<possiblePath.size()-1){
                List<Intersection> itinerary = map.getShortestPath(possiblePath.get(j).getDestination(), possiblePath.get(j+1).getDestination());
                List<Segment> itinerarySegments = map.getShortestSegmentsBetween(itinerary);
                for(int k=0; k<itinerarySegments.size(); k++){
                    currentCost+=itinerarySegments.get(k).length();
                }
                j++;
            }

            if (minimumCost>currentCost){
                minimumCost = currentCost;
                sortedRequests = possiblePath;
            }
        }

        return sortedRequests;
    }

    public List<List<DeliveryRequest>> generatePaths(List<DeliveryRequest> requests) {
        List<List<DeliveryRequest>> allPathPossibilities = new ArrayList<>();
        generate(new ArrayList<>(), requests, allPathPossibilities);
        return allPathPossibilities;
    }

    public void generate(List<DeliveryRequest> prefix, List<DeliveryRequest> rest, List<List<DeliveryRequest>> possibilities) {
        if (rest.size() <= 1) {
            List<DeliveryRequest> permutation = new ArrayList<>(prefix);
            permutation.addAll(rest);
            possibilities.add(permutation);
        } else {
            for (int i = 0; i < rest.size(); i++) {
                List<DeliveryRequest> pathPossibility = new ArrayList<>(prefix);
                pathPossibility.add(rest.get(i));

                List<DeliveryRequest> newRest = new ArrayList<>(rest.subList(0, i));
                newRest.addAll(rest.subList(i+1, rest.size()));

                generate(pathPossibility, newRest, possibilities);
            }
        }
    }
}

