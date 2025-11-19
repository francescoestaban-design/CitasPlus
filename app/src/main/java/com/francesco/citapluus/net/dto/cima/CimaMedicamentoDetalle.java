package com.francesco.citapluus.net.dto.cima;

import java.util.List;

public class CimaMedicamentoDetalle {
    public String nregistro;
    public String nombre;
    public String labtitular;
    public String labcomercializador;
    public String cpresc;
    public List<Documento> docs;

    public static class Documento {
        public String tipo;
        public String seccion;
        public String url;
    }
}
