package com.francesco.citapluus.net.dto;

public class CreateAppointmentReq {
    public String centerId;
    public String patientDni;
    public String startsAt; // ISO-8601
    public String endsAt;   // ISO-8601

    public CreateAppointmentReq(String centerId, String patientDni, String startsAt, String endsAt) {
        this.centerId = centerId;
        this.patientDni = patientDni;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }
}
