package br.com.pibic.api;

public class AcoesDisponiveis {

    private boolean conteudo;
    private boolean psicologo;
    private boolean localAtendimento;
    private boolean monitoramento;
    private boolean cvv;
    private String telefoneCvv;

    public AcoesDisponiveis() {
    }

    public AcoesDisponiveis(
            boolean conteudo,
            boolean psicologo,
            boolean localAtendimento,
            boolean monitoramento,
            boolean cvv,
            String telefoneCvv) {

        this.conteudo = conteudo;
        this.psicologo = psicologo;
        this.localAtendimento = localAtendimento;
        this.monitoramento = monitoramento;
        this.cvv = cvv;
        this.telefoneCvv = telefoneCvv;
    }

    public boolean isConteudo() {
        return conteudo;
    }

    public void setConteudo(boolean conteudo) {
        this.conteudo = conteudo;
    }

    public boolean isPsicologo() {
        return psicologo;
    }

    public void setPsicologo(boolean psicologo) {
        this.psicologo = psicologo;
    }

    public boolean isLocalAtendimento() {
        return localAtendimento;
    }

    public void setLocalAtendimento(boolean localAtendimento) {
        this.localAtendimento = localAtendimento;
    }

    public boolean isMonitoramento() {
        return monitoramento;
    }

    public void setMonitoramento(boolean monitoramento) {
        this.monitoramento = monitoramento;
    }

    public boolean isCvv() {
        return cvv;
    }

    public void setCvv(boolean cvv) {
        this.cvv = cvv;
    }

    public String getTelefoneCvv() {
        return telefoneCvv;
    }

    public void setTelefoneCvv(String telefoneCvv) {
        this.telefoneCvv = telefoneCvv;
    }
}