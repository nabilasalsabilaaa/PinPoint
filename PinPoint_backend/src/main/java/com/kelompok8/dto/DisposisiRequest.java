package com.kelompok8.dto;

public class DisposisiRequest {
    private Long idDinas;
    private String catatan;

    public Long getIdDinas() {
        return idDinas;
    }

    public void setIdDinas(Long idDinas) {
        this.idDinas = idDinas;
    }

    public String getCatatan() {
        return catatan;
    }

    public void setCatatan(String catatan) {
        this.catatan = catatan;
    }
}
