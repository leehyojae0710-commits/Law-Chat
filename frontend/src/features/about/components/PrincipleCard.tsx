interface PrincipleCardProps {
  order: number;
  title: string;
  description: string;
}

export const PrincipleCard = ({ order, title, description }: PrincipleCardProps) => {
  return (
    <div className="rounded-xl bg-purple-50 p-6">
      <div className="w-8 h-8 rounded-full bg-white flex items-center justify-center text-purple-600 font-bold mb-3">
        {order}
      </div>
      <p className="font-semibold mb-1">{title}</p>
      <p className="text-sm text-gray-600">{description}</p>
    </div>
  );
};
