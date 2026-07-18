import pandas as pd
import matplotlib.pyplot as plt
import plotly.express as px
import plotly.graph_objects as go
from plotly.subplots import make_subplots

def plot_loc_by_language(project_data):
    """Dile göre kod dağılımını gösteren etkileşimli pasta grafiği oluşturur."""
    # Bu fonksiyonun doğru veri alması için analysis_engine'in uygun veri üretmesi gerekir
    # Şimdilik örnek veri ile gösterim
    loc_data = project_data.get("loc_by_language", {})
    if not loc_data:
        # Örnek veri - gerçek implementasyon analysis_engine'e bağlı
        labels = ['Python', 'C#', 'JSON', 'Text']
        values = [3000, 2000, 500, 300]
    else:
        labels = list(loc_data.keys())
        values = list(loc_data.values())
    
    fig = px.pie(
        values=values,
        names=labels,
        title="Dile Göre Kod Dağılımı",
        color_discrete_sequence=px.colors.qualitative.Set3
    )
    fig.update_traces(textposition='inside', textinfo='percent+label')
    return fig

def plot_complexity(project_data):
    """Dosya karmaşıklıklarını gösteren etkileşimli bir bar grafiği oluşturur."""
    complexities = project_data.get("file_complexities", {})
    if not complexities:
        # Boş veri durumunda uygun mesaj içeren grafik
        fig = go.Figure()
        fig.add_annotation(text="Veri yok", xref="paper", yref="paper", x=0.5, y=0.5, showarrow=False, font=dict(size=20))
        fig.update_layout(title="En Karmaşık 10 Dosya", xaxis_title="Dosyalar", yaxis_title="Karmaşıklık")
        return fig

    # Sadece en karmaşık 10 dosyayı göster
    sorted_complexities = dict(sorted(complexities.items(), key=lambda item: item[1], reverse=True)[:10])
    
    fig = go.Figure(data=[
        go.Bar(
            x=list(sorted_complexities.keys()),
            y=list(sorted_complexities.values()),
            marker_color='indianred',
            text=list(sorted_complexities.values()),
            textposition='auto',
        )
    ])
    fig.update_layout(
        title="En Karmaşık 10 Dosya",
        xaxis_title="Dosyalar",
        yaxis_title="Karmaşıklık",
        xaxis_tickangle=-45
    )
    
    return fig

def plot_loc_over_time(history_data):
    """Zaman içindeki kod satırı artışını gösteren bir çizgi grafiği oluşturur."""
    if history_data.empty:
        # Boş bir grafik döndür
        fig = px.line(title='Veri yok')
        return fig

    # history_data: Pandas DataFrame formatında olmalı
    fig = px.line(
        history_data, 
        x='analysis_date', 
        y='code_lines', 
        title='Zaman İçinde Kod Satırı Gelişimi',
        line_shape='linear'
    )
    fig.update_traces(line=dict(width=3), mode='lines+markers')
    fig.update_layout(
        xaxis_title="Tarih",
        yaxis_title="Kod Satırları",
        hovermode='x'
    )
    return fig