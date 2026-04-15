import { Component } from '@angular/core';

interface ChartData {
  label: string;
  value: number;
  color: string;
}

interface StatCard {
  title: string;
  value: string;
  change: string;
  icon: string;
  color: string;
  period: string;
}

interface RecentActivity {
  id: string;
  user: string;
  action: string;
  target: string;
  time: string;
  avatar: string;
  type: string;
}

@Component({
  selector: 'app-statistics',
  templateUrl: './statistics.component.html',
  styleUrl: './statistics.component.scss'
})
export class StatisticsComponent {
  today: Date = new Date();
 mainStats: StatCard[] = [
  {
    title: 'Certifications issued',
    value: '2,847',
    change: '+12.5%',
    icon: '🎓',
    color: 'primary',
    period: 'vs last month'
  },
  {
    title: 'Active learners',
    value: '1,234',
    change: '+8.2%',
    icon: '👥',
    color: 'violet',
    period: 'vs last month'
  },
  {
    title: 'Success rate',
    value: '94%',
    change: '+5.1%',
    icon: '⭐',
    color: 'success',
    period: 'vs last month'
  },
  {
    title: 'Monthly revenue',
    value: '45.2K €',
    change: '+15.3%',
    icon: '💰',
    color: 'warning',
    period: 'vs last month'
  }
];

secondaryStats = [
  {
    label: 'Ongoing certifications',
    value: '156',
    icon: '📝',
    color: 'primary'
  },
  {
    label: 'Scheduled sessions',
    value: '23',
    icon: '📅',
    color: 'violet'
  },
  {
    label: 'Active trainers',
    value: '18',
    icon: '👨‍🏫',
    color: 'success'
  },
  {
    label: 'Pending evaluations',
    value: '12',
    icon: '📋',
    color: 'warning'
  }
];

  // Données du graphique d'évolution
  chartData: ChartData[] = [
    { label: 'Jan', value: 320, color: '#4facfe' },
    { label: 'Fév', value: 450, color: '#4facfe' },
    { label: 'Mar', value: 580, color: '#4facfe' },
    { label: 'Avr', value: 720, color: '#4facfe' },
    { label: 'Mai', value: 890, color: '#4facfe' },
    { label: 'Juin', value: 1240, color: '#4facfe' }
  ];

  // Top certifications
  topCertifications = [
    {
      name: 'AWS Solutions Architect',
      enrollments: 245,
      growth: '+23%',
      icon: '☁️',
      color: '#4facfe'
    },
    {
      name: 'Machine Learning',
      enrollments: 187,
      growth: '+45%',
      icon: '🤖',
      color: '#8a2be2'
    },
    {
      name: 'Certified Ethical Hacker',
      enrollments: 156,
      growth: '+32%',
      icon: '🔒',
      color: '#00f2fe'
    },
    {
      name: 'PMP Certification',
      enrollments: 124,
      growth: '+18%',
      icon: '📋',
      color: '#9370db'
    }
  ];

  // Activités récentes
  recentActivities: RecentActivity[] = [
    {
      id: '1',
      user: 'Ahmed Ben Salem',
      action: 'a obtenu la certification',
      target: 'AWS Solutions Architect',
      time: 'Il y a 15 min',
      avatar: 'AB',
      type: 'certification'
    },
    {
      id: '2',
      user: 'Sarra Mansour',
      action: "s'est inscrite à",
      target: 'Machine Learning',
      time: 'Il y a 35 min',
      avatar: 'SM',
      type: 'inscription'
    },
    {
      id: '3',
      user: 'Mehdi Trabelsi',
      action: 'a complété',
      target: 'Module Cybersécurité',
      time: 'Il y a 1h',
      avatar: 'MT',
      type: 'evaluation'
    },
    {
      id: '4',
      user: 'Nour Ben Ali',
      action: 'a effectué un paiement',
      target: 'Certification PMP',
      time: 'Il y a 2h',
      avatar: 'NB',
      type: 'paiement'
    },
    {
      id: '5',
      user: 'Fares Mezni',
      action: 'a rejoint comme',
      target: 'Formateur',
      time: 'Il y a 3h',
      avatar: 'FM',
      type: 'inscription'
    }
  ];

  // Alertes
  alerts = [
    {
      type: 'warning',
      message: '12 évaluations en attente de correction',
      time: 'Il y a 10 min',
      icon: '⚠️'
    },
    {
      type: 'info',
      message: '3 nouvelles inscriptions aujourd\'hui',
      time: 'Il y a 25 min',
      icon: 'ℹ️'
    },
    {
      type: 'success',
      message: 'Session DevOps complétée avec 98%',
      time: 'Il y a 1h',
      icon: '✅'
    }
  ];

  maxChartValue: number = 1500;

  ngOnInit() {
    this.maxChartValue = Math.max(...this.chartData.map(d => d.value)) * 1.2;
  }

  getStatColor(color: string): string {
    const colors = {
      primary: '#4facfe',
      violet: '#8a2be2',
      success: '#10b981',
      warning: '#f59e0b'
    };
    return colors[color as keyof typeof colors] || colors.primary;
  }

  getActivityIcon(type: string): string {
    const icons = {
      certification: '🎓',
      inscription: '📝',
      paiement: '💰',
      evaluation: '📋'
    };
    return icons[type as keyof typeof icons] || '📌';
  }

  getAlertClass(type: string): string {
    const classes = {
      warning: 'alert-warning',
      info: 'alert-info',
      success: 'alert-success'
    };
    return classes[type as keyof typeof classes] || 'alert-info';
  }
}
