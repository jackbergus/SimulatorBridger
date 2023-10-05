package uk.ncl.giacomobergami.components.iot_protocol;


public class XMPPProtocol extends IoTProtocol {
	private static final float BATTERY_DRAINAGE_RATE=1.50f;
	private static final float TRANSMISSION_SPEED =3.00f;

    public XMPPProtocol() {
    		super("XMPP",BATTERY_DRAINAGE_RATE, TRANSMISSION_SPEED);
    }

}