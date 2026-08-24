interface FeatureCardProps {
  order: number;
  title: string;
  description: string;
}

export const FeatureCard = ({ order, title, description }: FeatureCardProps) => {
  return (
    <div className="border rounded-xl p-6">
      <div className="w-8 h-8 rounded-full bg-purple-100 flex items-center justify-center text-purple-600 font-bold mb-3">
        {order}
      </div>
      <p className="font-semibold mb-1">{title}</p>
      <p className="text-sm text-gray-600">{description}</p>
    </div>
  );
};
