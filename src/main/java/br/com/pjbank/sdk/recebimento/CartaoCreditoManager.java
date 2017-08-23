package br.com.pjbank.sdk.recebimento;

import br.com.pjbank.sdk.api.PJBankClient;
import br.com.pjbank.sdk.auth.PJBankAuthenticatedService;
import br.com.pjbank.sdk.exceptions.PJBankException;
import br.com.pjbank.sdk.models.recebimento.CartaoCredito;
import br.com.pjbank.sdk.models.recebimento.TransacaoCartaoCredito;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

/**
 * @author Vinícius Silva <vinicius.silva@superlogica.com>
 * @version 1.0
 * @since 1.0
 */
public class CartaoCreditoManager extends PJBankAuthenticatedService {
    /**
     * EndPoint a ser requisitado na API
     */
    private String endPoint = "recebimentos/{{credencial}}";

    public CartaoCreditoManager(String credencial, String chave) {
        super(credencial, chave);

        this.endPoint = this.endPoint.replace("{{credencial}}", credencial);
    }

    /**
     * Gera um token de segurança com os dados do cartão para ser utilizado nas operações de cobrança/recebimento
     * @param cartaoCredito: cartão de crédito a ser tokenizado
     * @return String: token gerado para o cartão
     */
    public String tokenize(CartaoCredito cartaoCredito) throws IOException, PJBankException {
        this.endPoint = this.endPoint.concat("/tokens");

        PJBankClient client = new PJBankClient(this.endPoint);
        HttpPost httpPost = client.getHttpPostClient();
        httpPost.addHeader("x-chave", this.getChave());

        JSONObject params = new JSONObject();

        params.put("nome_cartao", cartaoCredito.getNome());
        params.put("numero_cartao", cartaoCredito.getNumero());
        params.put("mes_vencimento", cartaoCredito.getMesVencimento());
        params.put("ano_vencimento", cartaoCredito.getAnoVencimento());
        params.put("cpf_cartao", cartaoCredito.getCpfCnpj());
        params.put("email_cartao", cartaoCredito.getEmail());
        params.put("celular_cartao", cartaoCredito.getCelular());
        params.put("codigo_cvv", cartaoCredito.getCvv());

        httpPost.setEntity(new StringEntity(params.toString(), StandardCharsets.UTF_8));

        String response = EntityUtils.toString(client.doRequest(httpPost).getEntity());
        JSONObject responseObject = new JSONObject(response);

        return responseObject.getString("token_cartao");
    }

    /**
     * Realização a emissão de uma transação via cartão de crédito utilizando um token (gerado via tokenizar())
     * @param token: Token do cartão de crédito (gerado via tokenizar())
     * @param descricao: Descrição do pagamento
     * @param valor: Valor do pagamento
     * @param parcelas: Quantidade de parcelas
     * @return TransacaoCartaoCredito: dados da transação
     */
    public TransacaoCartaoCredito create(String token, String descricao, double valor, int parcelas)
            throws IOException, ParseException, PJBankException {
        this.endPoint = this.endPoint.concat("/transacoes");

        PJBankClient client = new PJBankClient(this.endPoint);
        HttpPost httpPost = client.getHttpPostClient();
        httpPost.addHeader("x-chave", this.getChave());

        JSONObject params = new JSONObject();

        params.put("token_cartao", token);
        params.put("descricao_pagamento", descricao);
        params.put("valor", valor);
        params.put("parcela", parcelas);

        httpPost.setEntity(new StringEntity(params.toString(), StandardCharsets.UTF_8));

        String response = EntityUtils.toString(client.doRequest(httpPost).getEntity());
        JSONObject responseObject = new JSONObject(response);
        DateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

        TransacaoCartaoCredito transacaoCartaoCredito = new TransacaoCartaoCredito();
        transacaoCartaoCredito.setId(responseObject.getString("tid"));

        transacaoCartaoCredito.setPrevisaoCredito(dateFormat.parse(responseObject.getString("previsao_credito")));
        transacaoCartaoCredito.setIdConciliacao(responseObject.getString("tid_conciliacao"));
        transacaoCartaoCredito.setBandeira(responseObject.getString("bandeira"));
        transacaoCartaoCredito.setAutorizacao(responseObject.getString("autorizacao"));
        transacaoCartaoCredito.setCartaoTruncado(responseObject.getString("cartao_truncado"));
        transacaoCartaoCredito.setStatusCartao(responseObject.getInt("statuscartao"));
        transacaoCartaoCredito.setTarifa(responseObject.getDouble("tarifa"));
        transacaoCartaoCredito.setTaxa(responseObject.getDouble("taxa"));

        return transacaoCartaoCredito;
    }
}
