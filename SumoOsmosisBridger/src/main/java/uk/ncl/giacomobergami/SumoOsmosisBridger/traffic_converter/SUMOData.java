package uk.ncl.giacomobergami.SumoOsmosisBridger.traffic_converter;

import uk.ncl.giacomobergami.utils.shared_data.iot.TimedIoT;

import java.util.ArrayList;

public class SUMOData  {
  public ArrayList<TimedIoT> getSUMOData() {
    return SUMOData;
  }

  public void setSUMOData(ArrayList<TimedIoT> SUMOData) {
    this.SUMOData = SUMOData;
  }

  public void sdAddTo(TimedIoT add) {
      this.SUMOData.add(add);
  }

  private ArrayList<TimedIoT> SUMOData;

}
