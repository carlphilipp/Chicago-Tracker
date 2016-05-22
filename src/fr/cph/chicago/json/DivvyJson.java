package fr.cph.chicago.json;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

import fr.cph.chicago.entity.BikeStation;
import lombok.Data;

@Data
public class DivvyJson {

    @JsonProperty("executionTime")
    private String executionTime;
    @JsonProperty("stationBeanList")
    private List<BikeStation> stations;

    public DivvyJson() {
    }
}
