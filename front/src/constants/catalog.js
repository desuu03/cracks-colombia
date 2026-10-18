// Closed lists for sport, position and region, so search filters match what
// athletes entered. The API stores them as plain strings.
export const POSITIONS = {
  Fútbol: ['Portero', 'Defensa central', 'Lateral', 'Volante', 'Extremo', 'Delantero'],
  Baloncesto: ['Base', 'Escolta', 'Alero', 'Ala-pívot', 'Pívot'],
  Voleibol: ['Armador', 'Opuesto', 'Central', 'Punta', 'Líbero'],
  Atletismo: ['Velocidad 100 m', 'Velocidad 200 m', 'Medio fondo 800 m', 'Fondo 5.000 m', 'Salto largo', 'Lanzamiento de jabalina'],
}

export const SPORTS = Object.keys(POSITIONS)

export const REGIONS = [
  'Amazonas', 'Antioquia', 'Arauca', 'Atlántico', 'Bogotá D.C.', 'Bolívar', 'Boyacá', 'Caldas',
  'Caquetá', 'Casanare', 'Cauca', 'Cesar', 'Chocó', 'Córdoba', 'Cundinamarca', 'Guainía',
  'Guaviare', 'Huila', 'La Guajira', 'Magdalena', 'Meta', 'Nariño', 'Norte de Santander',
  'Putumayo', 'Quindío', 'Risaralda', 'San Andrés y Providencia', 'Santander', 'Sucre',
  'Tolima', 'Valle del Cauca', 'Vaupés', 'Vichada',
]
