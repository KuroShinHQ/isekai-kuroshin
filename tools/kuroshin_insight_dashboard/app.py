import streamlit as st
import analysis_engine

# Sayfa ayarlarını ve başlığını belirliyoruz (daha profesyonel bir görünüm için)
st.set_page_config(
    layout="wide",
    page_title="Kuroshin Insight Dashboard",
    page_icon="📊"
)

# --- KENAR ÇUBUĞU (SIDEBAR) ---
with st.sidebar:
    st.image("https://i.imgur.com/gJZGk1k.png", width=100) # Küçük bir logo ekleyebiliriz
    st.title("Proje Ayarları")
    
    # Kullanıcıdan analiz edilecek proje klasörünün yolunu alıyoruz
    project_path = st.text_input(
        "Proje Klasör Yolunu Girin:",
        "C:\\Users\\pc\\AndroidStudioProjects\\IsekaiKuroshin"  # Burası artık DOĞRU yolu gösteriyor
    )
    
    analyze_button = st.button("🚀 Projeyi Analiz Et", type="primary")
    
    st.info("💡 **Kullanım:** Proje yolunu girin ve 'Projeyi Analiz Et' butonuna tıklayın. Analiz tamamlandıktan sonra sonuçlar ana ekranda gösterilecektir.")


# --- ANA EKRAN ---
st.title("📊 Kuroshin Insight Dashboard")

# Eğer analiz butona basıldıysa
if analyze_button:
    # `session_state` kullanarak analiz sonuçlarını hafızada tutuyoruz
    with st.spinner("Kod tabanı taranıyor, analiz ediliyor..."):
        project_type, total_lines, lang_stats = analysis_engine.analyze_loc(project_path)
        
        # Hata varsa state'e hatayı, yoksa veriyi kaydet
        if "error" in lang_stats:
            st.session_state['analysis_results'] = None
            st.session_state['error_message'] = lang_stats["error"]
        else:
            st.session_state['analysis_results'] = {
                "project_type": project_type,
                "total_lines": total_lines,
                "lang_stats": lang_stats
            }
            st.session_state['error_message'] = None


# --- SONUÇLARI GÖSTERME BÖLÜMÜ ---

# Eğer bir hata mesajı varsa göster
if 'error_message' in st.session_state and st.session_state['error_message']:
    st.error(f"❌ **Analiz sırasında bir hata oluştu:** {st.session_state['error_message']}")

# Eğer hafızada başarılı bir analiz sonucu varsa göster
elif 'analysis_results' in st.session_state and st.session_state['analysis_results']:
    results = st.session_state['analysis_results']
    st.success("✅ Analiz başarıyla tamamlandı!")
    
    st.header("Proje Tanımı")
    st.info(f"**Proje Türü:** {results['project_type']}")
    
    st.header("Genel Bakış")
    col1, col2, col3 = st.columns(3)
    col1.metric(label="Toplam Kod Satırı", value=f"{results['total_lines']:,}")
    col2.metric(label="Farklı Dil Sayısı", value=len(results['lang_stats']))

    st.header("Dil Dağılımı")
    st.bar_chart(results['lang_stats'])
    st.write("Detaylı Döküm:")
    st.table(results['lang_stats'])

# Eğer henüz analiz yapılmadıysa hoş geldin mesajı göster
else:
    st.info("Kuroshin Insight Dashboard'a Hoş Geldiniz!")
    st.write("Bu panel, Isekai Kuroshin projesinin kod tabanının sağlığını, gelişimini ve yapısal metriklerini görselleştirmek için tasarlanmıştır.")
    st.warning("⬅️ Lütfen sol menüden proje klasörünün yolunu girin ve analizi başlatın.")