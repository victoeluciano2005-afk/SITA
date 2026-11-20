package com.example.data

import java.text.Normalizer

data class BrazilianState(
    val uf: String,
    val nome: String,
    val regiao: String,
    val biomaPrincipal: String,
    val capital: String,
    val cidadesComuns: List<String>
)

data class VegetableSuggestion(
    val nome: String,
    val categoria: String
)

enum class LocalCultivo(val label: String, val descricaoCurta: String) {
    CANTEIRO("Canteiro", "Direto no solo ou elevado"),
    VASO("Vaso", "Varanda, quintal ou janela"),
    HORTA_VERTICAL("Horta vertical", "Parede, painéis ou garrafas"),
    ESTUFA("Estufa", "Ambiente protegido e controlado");

    companion object {
        fun fromLabel(raw: String): LocalCultivo =
            entries.firstOrNull { it.label.equals(raw, ignoreCase = true) } ?: CANTEIRO
    }
}

object BrazilCatalog {

    val meses: List<String> = listOf(
        "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
        "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
    )

    val hortalicasPopulares: List<String> = listOf(
        "Alface", "Tomate", "Cenoura", "Couve-manteiga", "Cebolinha",
        "Coentro", "Pimentão", "Abóbora", "Quiabo", "Rúcula", "Manjericão", "Morango"
    )

    val hortalicas: List<VegetableSuggestion> = listOf(
        // Folhosas & PANCs famosas do Brasil
        VegetableSuggestion("Alface", "Folhosa"),
        VegetableSuggestion("Alface Crespa", "Folhosa"),
        VegetableSuggestion("Alface Americana", "Folhosa"),
        VegetableSuggestion("Alface Lisa", "Folhosa"),
        VegetableSuggestion("Alface Roxa", "Folhosa"),
        VegetableSuggestion("Alface Romana", "Folhosa"),
        VegetableSuggestion("Couve", "Folhosa"),
        VegetableSuggestion("Couve-manteiga", "Folhosa"),
        VegetableSuggestion("Couve-chinesa (Acelga chinesa)", "Folhosa"),
        VegetableSuggestion("Couve-de-bruxelas", "Folhosa"),
        VegetableSuggestion("Rúcula", "Folhosa"),
        VegetableSuggestion("Agrião", "Folhosa"),
        VegetableSuggestion("Espinafre", "Folhosa"),
        VegetableSuggestion("Acelga", "Folhosa"),
        VegetableSuggestion("Almeirão", "Folhosa"),
        VegetableSuggestion("Chicória", "Folhosa"),
        VegetableSuggestion("Escarola", "Folhosa"),
        VegetableSuggestion("Repolho Verde", "Folhosa"),
        VegetableSuggestion("Repolho Roxo", "Folhosa"),
        VegetableSuggestion("Mostarda", "Folhosa"),
        VegetableSuggestion("Serralha", "Folhosa / PANC"),
        VegetableSuggestion("Taioba", "Folhosa / PANC"),
        VegetableSuggestion("Ora-pro-nóbis", "Folhosa / PANC"),
        VegetableSuggestion("Bertalha", "Folhosa / PANC"),
        VegetableSuggestion("Jambu", "Folhosa / Norte"),
        VegetableSuggestion("Vinagreira (Cuxá)", "Folhosa / Nordeste"),
        VegetableSuggestion("Caruru (Bredo)", "Folhosa / PANC"),
        VegetableSuggestion("Peixinho-da-horta", "Folhosa / PANC"),

        // Frutos & Legumes clássicos
        VegetableSuggestion("Tomate", "Fruto"),
        VegetableSuggestion("Tomate Cereja", "Fruto"),
        VegetableSuggestion("Tomate Italiano", "Fruto"),
        VegetableSuggestion("Tomate Caqui", "Fruto"),
        VegetableSuggestion("Pimentão Verde", "Fruto"),
        VegetableSuggestion("Pimentão Vermelho", "Fruto"),
        VegetableSuggestion("Pimentão Amarelo", "Fruto"),
        VegetableSuggestion("Abóbora", "Fruto"),
        VegetableSuggestion("Abóbora Cabotiá (Japonesa)", "Fruto"),
        VegetableSuggestion("Abóbora Moranga", "Fruto"),
        VegetableSuggestion("Abóbora Menina Brasileira", "Fruto"),
        VegetableSuggestion("Abobrinha Italiana", "Fruto"),
        VegetableSuggestion("Pepino", "Fruto"),
        VegetableSuggestion("Pepino Japonês", "Fruto"),
        VegetableSuggestion("Pepino Caipira", "Fruto"),
        VegetableSuggestion("Quiabo", "Fruto"),
        VegetableSuggestion("Berinjela", "Fruto"),
        VegetableSuggestion("Jiló", "Fruto"),
        VegetableSuggestion("Chuchu", "Fruto"),
        VegetableSuggestion("Maxixe", "Fruto / Nordeste"),
        VegetableSuggestion("Morango", "Fruto"),
        VegetableSuggestion("Melancia", "Fruto"),
        VegetableSuggestion("Melão", "Fruto"),
        VegetableSuggestion("Milho-verde", "Grão / Espiga"),
        VegetableSuggestion("Feijão-vagem", "Legume"),
        VegetableSuggestion("Feijão-de-corda", "Legume / Nordeste"),
        VegetableSuggestion("Ervilha", "Legume"),
        VegetableSuggestion("Fava", "Legume"),

        // Raízes, Tubérculos & Bulbos
        VegetableSuggestion("Cenoura", "Raiz"),
        VegetableSuggestion("Beterraba", "Raiz"),
        VegetableSuggestion("Rabanete", "Raiz"),
        VegetableSuggestion("Nabo", "Raiz"),
        VegetableSuggestion("Batata-doce", "Raiz"),
        VegetableSuggestion("Batata-inglesa", "Tubérculo"),
        VegetableSuggestion("Mandioquinha (Batata-baroa)", "Raiz"),
        VegetableSuggestion("Mandioca (Aipim / Macaxeira)", "Raiz"),
        VegetableSuggestion("Inhame", "Tubérculo"),
        VegetableSuggestion("Cará", "Tubérculo"),
        VegetableSuggestion("Gengibre", "Rizoma"),
        VegetableSuggestion("Cúrcuma (Açafrão-da-terra)", "Rizoma"),
        VegetableSuggestion("Cebola", "Bulbo"),
        VegetableSuggestion("Cebola Roxa", "Bulbo"),
        VegetableSuggestion("Alho", "Bulbo"),
        VegetableSuggestion("Alho-poró", "Bulbo"),

        // Flores & Talos
        VegetableSuggestion("Brócolis", "Flor"),
        VegetableSuggestion("Couve-flor", "Flor"),
        VegetableSuggestion("Alcachofra", "Flor"),
        VegetableSuggestion("Aipo (Salsão)", "Talo"),
        VegetableSuggestion("Erva-doce (Funcho)", "Talo / Tempero"),

        // Temperos, Ervas & Pimentas do Brasil
        VegetableSuggestion("Cebolinha", "Tempero"),
        VegetableSuggestion("Salsinha", "Tempero"),
        VegetableSuggestion("Coentro", "Tempero"),
        VegetableSuggestion("Chicória-do-pará (Coentro-bravo)", "Tempero / Norte"),
        VegetableSuggestion("Manjericão", "Tempero"),
        VegetableSuggestion("Manjericão Roxo", "Tempero"),
        VegetableSuggestion("Hortelã", "Tempero / Chá"),
        VegetableSuggestion("Alecrim", "Tempero"),
        VegetableSuggestion("Orégano", "Tempero"),
        VegetableSuggestion("Tomilho", "Tempero"),
        VegetableSuggestion("Sálvia", "Tempero"),
        VegetableSuggestion("Manjerona", "Tempero"),
        VegetableSuggestion("Louro", "Tempero"),
        VegetableSuggestion("Pimenta Dedo-de-Moça", "Pimenta"),
        VegetableSuggestion("Pimenta Biquinho", "Pimenta"),
        VegetableSuggestion("Pimenta Malagueta", "Pimenta"),
        VegetableSuggestion("Pimenta-de-cheiro", "Pimenta / Norte-NE"),
        VegetableSuggestion("Pimenta Cambuci", "Pimenta"),
        VegetableSuggestion("Erva-cidreira (Melissa)", "Erva / Chá"),
        VegetableSuggestion("Capim-limão (Capim-santo)", "Erva / Chá"),
        VegetableSuggestion("Camomila", "Erva / Chá"),
        VegetableSuggestion("Boldo-brasileiro", "Erva medicinal")
    )

    val estados: List<BrazilianState> = listOf(
        BrazilianState("AC", "Acre", "Norte", "Amazônia", "Rio Branco", listOf("Rio Branco", "Cruzeiro do Sul", "Sena Madureira")),
        BrazilianState("AL", "Alagoas", "Nordeste", "Mata Atlântica / Caatinga", "Maceió", listOf("Maceió", "Arapiraca", "Palmeira dos Índios")),
        BrazilianState("AP", "Amapá", "Norte", "Amazônia", "Macapá", listOf("Macapá", "Santana", "Laranjal do Jari")),
        BrazilianState("AM", "Amazonas", "Norte", "Amazônia", "Manaus", listOf("Manaus", "Parintins", "Itacoatiara")),
        BrazilianState("BA", "Bahia", "Nordeste", "Caatinga / Mata Atlântica / Cerrado", "Salvador", listOf("Salvador", "Feira de Santana", "Vitória da Conquista", "Juazeiro", "Barreiras")),
        BrazilianState("CE", "Ceará", "Nordeste", "Caatinga", "Fortaleza", listOf("Fortaleza", "Juazeiro do Norte", "Sobral", "Quixadá")),
        BrazilianState("DF", "Distrito Federal", "Centro-Oeste", "Cerrado", "Brasília", listOf("Brasília", "Taguatinga", "Planaltina")),
        BrazilianState("ES", "Espírito Santo", "Sudeste", "Mata Atlântica", "Vitória", listOf("Vitória", "Vila Velha", "Cachoeiro de Itapemirim", "Linhares")),
        BrazilianState("GO", "Goiás", "Centro-Oeste", "Cerrado", "Goiânia", listOf("Goiânia", "Anápolis", "Rio Verde", "Aparecida de Goiânia")),
        BrazilianState("MA", "Maranhão", "Nordeste", "Amazônia / Cerrado", "São Luís", listOf("São Luís", "Imperatriz", "Caxias")),
        BrazilianState("MT", "Mato Grosso", "Centro-Oeste", "Cerrado / Amazônia / Pantanal", "Cuiabá", listOf("Cuiabá", "Rondonópolis", "Sinop", "Cáceres")),
        BrazilianState("MS", "Mato Grosso do Sul", "Centro-Oeste", "Cerrado / Pantanal", "Campo Grande", listOf("Campo Grande", "Dourados", "Corumbá", "Três Lagoas")),
        BrazilianState("MG", "Minas Gerais", "Sudeste", "Cerrado / Mata Atlântica", "Belo Horizonte", listOf("Belo Horizonte", "Uberlândia", "Juiz de Fora", "Montes Claros", "Poços de Caldas")),
        BrazilianState("PA", "Pará", "Norte", "Amazônia", "Belém", listOf("Belém", "Santarém", "Marabá", "Ananindeua")),
        BrazilianState("PB", "Paraíba", "Nordeste", "Caatinga / Mata Atlântica", "João Pessoa", listOf("João Pessoa", "Campina Grande", "Patos")),
        BrazilianState("PR", "Paraná", "Sul", "Mata Atlântica", "Curitiba", listOf("Curitiba", "Londrina", "Maringá", "Cascavel", "Ponta Grossa")),
        BrazilianState("PE", "Pernambuco", "Nordeste", "Caatinga / Mata Atlântica", "Recife", listOf("Recife", "Caruaru", "Petrolina", "Olinda")),
        BrazilianState("PI", "Piauí", "Nordeste", "Caatinga / Cerrado", "Teresina", listOf("Teresina", "Parnaíba", "Picos")),
        BrazilianState("RJ", "Rio de Janeiro", "Sudeste", "Mata Atlântica", "Rio de Janeiro", listOf("Rio de Janeiro", "Niterói", "Petrópolis", "Campos dos Goytacazes")),
        BrazilianState("RN", "Rio Grande do Norte", "Nordeste", "Caatinga / Mata Atlântica", "Natal", listOf("Natal", "Mossoró", "Parnamirim")),
        BrazilianState("RS", "Rio Grande do Sul", "Sul", "Pampa / Mata Atlântica", "Porto Alegre", listOf("Porto Alegre", "Caxias do Sul", "Pelotas", "Santa Maria", "Passo Fundo")),
        BrazilianState("RO", "Rondônia", "Norte", "Amazônia", "Porto Velho", listOf("Porto Velho", "Ji-Paraná", "Ariquemes")),
        BrazilianState("RR", "Roraima", "Norte", "Amazônia", "Boa Vista", listOf("Boa Vista", "Rorainópolis")),
        BrazilianState("SC", "Santa Catarina", "Sul", "Mata Atlântica", "Florianópolis", listOf("Florianópolis", "Joinville", "Blumenau", "Chapecó", "Lages")),
        BrazilianState("SP", "São Paulo", "Sudeste", "Mata Atlântica / Cerrado", "São Paulo", listOf("São Paulo", "Campinas", "Ribeirão Preto", "Santos", "São José dos Campos")),
        BrazilianState("SE", "Sergipe", "Nordeste", "Mata Atlântica / Caatinga", "Aracaju", listOf("Aracaju", "Itabaiana", "Lagarto")),
        BrazilianState("TO", "Tocantins", "Norte", "Cerrado / Amazônia", "Palmas", listOf("Palmas", "Araguaína", "Gurupi"))
    )

    fun normalizeForSearch(input: String): String {
        val decomposed = Normalizer.normalize(input.trim().lowercase(), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    fun filterHortalicas(query: String): List<VegetableSuggestion> {
        val clean = normalizeForSearch(query)
        if (clean.isEmpty()) return hortalicas.take(12)
        val startsWith = hortalicas.filter {
            normalizeForSearch(it.nome).startsWith(clean)
        }
        val contains = hortalicas.filter {
            !normalizeForSearch(it.nome).startsWith(clean) &&
                (normalizeForSearch(it.nome).contains(clean) ||
                    normalizeForSearch(it.categoria).contains(clean))
        }
        return (startsWith + contains).take(12)
    }

    fun findStateByNameOrUf(query: String?): BrazilianState? {
        if (query.isNullOrBlank()) return null
        val clean = normalizeForSearch(query)
        return estados.firstOrNull {
            normalizeForSearch(it.uf) == clean ||
                normalizeForSearch(it.nome) == clean ||
                normalizeForSearch(it.nome).contains(clean)
        }
    }
}
