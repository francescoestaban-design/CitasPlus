package com.francesco.citapluus.net.dto.cima;

import java.util.List;

public class CimaMedicamentoResumen {
    public String nregistro;
    public String nombre;
    public String labtitular;
    public String labcomercializador;
    public String cpresc;
    public Estado estado;
    public boolean comerc;
    public boolean receta;
    public boolean generico;
    public boolean conduc;
    public boolean triangulo;
    public boolean huerfano;
    public boolean biosimilar;
    public boolean psum;
    public boolean notas;
    public boolean materialesInf;
    public boolean ema;
    public List<Documento> docs;
    public FormaFarmaceutica formaFarmaceutica;
    public FormaFarmaceuticaSimplificada formaFarmaceuticaSimplificada;
    public VTM vtm;
    public String dosis;

    public static class Estado {
        public long aut;
        public Long rev; // puede ser null
    }

    public static class Documento {
        public int tipo;
        public String url;
        public String urlHtml;
        public boolean secc;
        public Long fecha;
    }

    public static class FormaFarmaceutica {
        public int id;
        public String nombre;
    }

    public static class FormaFarmaceuticaSimplificada {
        public int id;
        public String nombre;
    }

    public static class VTM {
        public long id;
        public String nombre;
    }
}
